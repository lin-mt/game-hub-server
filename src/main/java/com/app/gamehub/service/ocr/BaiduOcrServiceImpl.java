package com.app.gamehub.service.ocr;

import com.app.gamehub.entity.OcrQuota;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.exception.OcrAttributeException;
import com.app.gamehub.repository.OcrQuotaRepository;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 百度OCR服务实现 */
@Slf4j
@Service("baiduOcrService")
public class BaiduOcrServiceImpl implements OcrService {

  private static final String PROVIDER_NAME = "baidu";
  private static final String API_KEY = "nzGHDeN4xeCRUZ1bcP9e6IRS";
  private static final String SECRET_KEY = "qCdOvBFyEspvK8flT5T0BGLvspD5RQAf";

  private static final OkHttpClient HTTP_CLIENT =
      new OkHttpClient().newBuilder().readTimeout(300, TimeUnit.SECONDS).build();

  // QPS限制：每秒最多2次请求，即每次请求间隔至少500ms
  private static final long QPS_INTERVAL_MS = 500;

  private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d{1,4})(?:\\.\\d+)?%");

  // 缓存access token
  private String cachedAccessToken = null;
  private long tokenExpireTime = 0;

  @Autowired private OcrQuotaRepository quotaRepository;

  /** 百度OCR服务类型枚举 */
  @Getter
  public enum BaiduOcrServiceType {
    GENERAL_BASIC("general_basic", "通用文字识别（标准版）", 1000, "/rest/2.0/ocr/v1/general_basic"),
    GENERAL("general", "通用文字识别（标准含位置版）", 1000, "/rest/2.0/ocr/v1/general"),
    ACCURATE_BASIC("accurate_basic", "通用文字识别（高精度版）", 1000, "/rest/2.0/ocr/v1/accurate_basic"),
    ACCURATE("accurate", "通用文字识别（高精度含位置版）", 500, "/rest/2.0/ocr/v1/accurate"),
    WEBIMAGE("webimage", "网络图片文字识别", 1000, "/rest/2.0/ocr/v1/webimage");

    private final String code;
    private final String name;
    private final int monthlyQuota;
    private final String apiPath;

    BaiduOcrServiceType(String code, String name, int monthlyQuota, String apiPath) {
      this.code = code;
      this.name = name;
      this.monthlyQuota = monthlyQuota;
      this.apiPath = apiPath;
    }
  }

  @Override
  @Transactional
  public Map<String, Integer> parseStats(File imageFile) throws Exception {
    String currentMonth = java.time.YearMonth.now().toString();

    // 初始化本月额度记录
    initMonthQuotas(currentMonth);

    // 裁剪图片左半部分
    File croppedFile = null;
    try {
      croppedFile = com.app.gamehub.util.ImageUtils.cropLeftHalf(imageFile);
      log.debug("百度OCR图片裁剪成功，原始文件: {}, 裁剪后文件: {}", imageFile.getName(), croppedFile.getName());

      // 获取可用的服务列表
      List<OcrQuota> availableQuotas =
          quotaRepository.findAvailableQuotasByProviderAndMonth(PROVIDER_NAME, currentMonth);

      if (availableQuotas.isEmpty()) {
        throw new BusinessException("OCR服务的免费额度已用完");
      }

      // 尝试使用每个可用的服务
      Exception lastException = null;
      for (OcrQuota quota : availableQuotas) {
        try {
          log.info(
              "尝试使用百度OCR服务: {} (剩余额度: {}/{})",
              quota.getServiceType(),
              quota.getRemainingQuota(),
              quota.getTotalQuota());

          // 执行OCR识别
          Map<String, Integer> result = performOcr(croppedFile, quota);

          // 使用数据库原子操作更新额度
          updateQuotaAtomic(quota.getId());

          log.info("百度OCR识别成功，使用服务: {}", quota.getServiceType());
          return result;

        } catch (OcrAttributeException oae) {
          // 属性数据错误，直接抛出不再尝试其他服务
          log.error("百度OCR服务 {} 属性数据错误，直接抛出异常", quota.getServiceType(), oae);
          throw oae;
        } catch (BusinessException be) {
          lastException = be;
          log.warn("百度OCR服务 {} 识别失败: {}", quota.getServiceType(), be.getMessage());

          if (be.getMessage().contains("QPS")) {
            continue;
          }
          if (be.getMessage().contains("额度")) {
            markQuotaExhausted(quota.getId());
          }
        } catch (Exception e) {
          lastException = e;
          log.warn("百度OCR服务 {} 调用异常: {}", quota.getServiceType(), e.getMessage());
        }
      }

      throw new BusinessException("OCR所有服务都不可用: " + (lastException != null ? lastException.getMessage() : "未知错误"));
    } finally {
      // 清理裁剪后的临时文件
      if (croppedFile != null && croppedFile.exists()) {
        try {
          java.nio.file.Files.deleteIfExists(croppedFile.toPath());
          log.debug("已删除百度OCR裁剪后的临时文件: {}", croppedFile.getName());
        } catch (Exception e) {
          log.warn("删除百度OCR裁剪后的临时文件失败: {}", e.getMessage());
        }
      }
    }
  }

  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  @Override
  @Transactional
  public boolean isAvailable() {
    try {
      String currentMonth = java.time.YearMonth.now().toString();

      // 先初始化本月额度记录（如果不存在）
      initMonthQuotas(currentMonth);

      List<OcrQuota> availableQuotas =
          quotaRepository.findAvailableQuotasByProviderAndMonth(PROVIDER_NAME, currentMonth);
      return !availableQuotas.isEmpty();
    } catch (Exception e) {
      log.error("检查百度OCR可用性失败", e);
      return false;
    }
  }

  /** 执行OCR识别 */
  private Map<String, Integer> performOcr(File imageFile, OcrQuota quota) throws Exception {
    BaiduOcrServiceType serviceType = getServiceTypeByCode(quota.getServiceType());

    // QPS限流
    enforceQpsLimit(quota);

    // 获取access token
    String accessToken = getAccessToken();

    // 读取图片并转换为base64
    String imageBase64 = getFileContentAsBase64(imageFile.getAbsolutePath(), true);

    // 调用百度OCR API
    MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
    RequestBody body = RequestBody.create(mediaType, "image=" + imageBase64);

    Request request =
        new Request.Builder()
            .url(
                "https://aip.baidubce.com"
                    + serviceType.getApiPath()
                    + "?access_token="
                    + accessToken)
            .method("POST", body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .build();

    Response response = HTTP_CLIENT.newCall(request).execute();
    String responseBody = response.body().string();

    // 解析响应
    JSONObject jsonResponse = new JSONObject(responseBody);

    // 检查是否有错误
    if (jsonResponse.has("error_code")) {
      int errorCode = jsonResponse.getInt("error_code");
      String errorMsg = jsonResponse.optString("error_msg", "未知错误");

      if (errorCode == 18) {
        throw new BusinessException("OCR服务QPS超限");
      } else if (errorCode == 4 || errorCode == 17 || errorCode == 19) {
        throw new BusinessException("OCR服务免费额度已用完");
      } else {
        throw new BusinessException("OCR识别失败: " + errorMsg);
      }
    }

    // 提取识别结果
    JSONArray wordsResult = jsonResponse.getJSONArray("words_result");
    StringBuilder textBuilder = new StringBuilder();
    for (int i = 0; i < wordsResult.length(); i++) {
      JSONObject item = wordsResult.getJSONObject(i);
      textBuilder.append(item.getString("words")).append(" ");
    }

    String resultText = textBuilder.toString().replace(" ", "").replace("_", "");

    // 解析属性
    return extractStats(resultText);
  }

  /** QPS限流 */
  private synchronized void enforceQpsLimit(OcrQuota quota) {
    long now = System.currentTimeMillis();
    long lastTime = quota.getLastRequestTime();
    long elapsed = now - lastTime;

    if (elapsed < QPS_INTERVAL_MS) {
      try {
        long sleepTime = QPS_INTERVAL_MS - elapsed;
        log.debug("百度OCR服务 {} QPS限流，等待{}ms", quota.getServiceType(), sleepTime);
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    quota.setLastRequestTime(System.currentTimeMillis());
  }

  /** 使用数据库原子操作更新额度 */
  @Transactional
  public void updateQuotaAtomic(Long quotaId) {
    try {
      int updated = quotaRepository.incrementUsedQuota(quotaId);
      if (updated > 0) {
        log.debug("百度OCR原子更新额度统计成功，ID: {}", quotaId);
      } else {
        log.warn("百度OCR原子更新额度统计失败（可能额度已用完），ID: {}", quotaId);
      }
    } catch (Exception e) {
      log.error("百度OCR原子更新额度统计异常", e);
    }
  }

  /** 标记额度已用完 */
  @Transactional
  public void markQuotaExhausted(Long quotaId) {
    try {
      OcrQuota quota =
          quotaRepository.findById(quotaId).orElseThrow(() -> new BusinessException("额度记录不存在"));
      quota.setRemainingQuota(0);
      quota.setUsedQuota(quota.getTotalQuota());
      quotaRepository.save(quota);
      log.info("标记百度OCR服务 {} 额度已用完", quota.getServiceType());
    } catch (Exception e) {
      log.error("标记百度OCR额度失败", e);
    }
  }

  /** 初始化本月额度记录 */
  @Transactional
  public void initMonthQuotas(String month) {
    for (BaiduOcrServiceType serviceType : BaiduOcrServiceType.values()) {
      Optional<OcrQuota> existing =
          quotaRepository.findByProviderAndServiceTypeAndQuotaMonthWithLock(
              PROVIDER_NAME, serviceType.getCode(), month);

      if (existing.isEmpty()) {
        OcrQuota quota = new OcrQuota();
        quota.setProvider(PROVIDER_NAME);
        quota.setServiceType(serviceType.getCode());
        quota.setQuotaMonth(month);
        quota.setTotalQuota(serviceType.getMonthlyQuota());
        quota.setUsedQuota(0);
        quota.setRemainingQuota(serviceType.getMonthlyQuota());
        quota.setLastRequestTime(0L);
        quotaRepository.save(quota);
        log.info("初始化百度OCR服务 {} 的本月额度: {}", serviceType.getName(), serviceType.getMonthlyQuota());
      }
    }
  }

  /** 获取服务类型枚举 */
  private BaiduOcrServiceType getServiceTypeByCode(String code) {
    for (BaiduOcrServiceType type : BaiduOcrServiceType.values()) {
      if (type.getCode().equals(code)) {
        return type;
      }
    }
    return BaiduOcrServiceType.ACCURATE_BASIC;
  }

  /** 获取文件base64编码 */
  private String getFileContentAsBase64(String path, boolean urlEncode) throws IOException {
    byte[] b = Files.readAllBytes(new File(path).toPath());
    String base64 = Base64.getEncoder().encodeToString(b);
    if (urlEncode) {
      base64 = URLEncoder.encode(base64, StandardCharsets.UTF_8);
    }
    return base64;
  }

  /** 获取Access Token（带缓存） */
  private synchronized String getAccessToken() throws IOException {
    long now = System.currentTimeMillis();

    if (cachedAccessToken != null && now < tokenExpireTime) {
      return cachedAccessToken;
    }

    MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
    RequestBody body =
        RequestBody.create(
            mediaType,
            "grant_type=client_credentials&client_id=" + API_KEY + "&client_secret=" + SECRET_KEY);

    Request request =
        new Request.Builder()
            .url("https://aip.baidubce.com/oauth/2.0/token")
            .method("POST", body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();

    Response response = HTTP_CLIENT.newCall(request).execute();
    JSONObject jsonResponse = new JSONObject(response.body().string());

    cachedAccessToken = jsonResponse.getString("access_token");
    tokenExpireTime = now + 29L * 24 * 60 * 60 * 1000;

    log.info("获取新的百度OCR Access Token");
    return cachedAccessToken;
  }

  /** 从识别文本中提取属性数据 */
  private Map<String, Integer> extractStats(String text) {
    Map<String, Integer> result = new LinkedHashMap<>();

    List<Integer> values = new ArrayList<>();
    Matcher matcher = PERCENT_PATTERN.matcher(text);

    while (matcher.find()) {
      values.add(Integer.parseInt(matcher.group(1)));
    }

    int count = values.size();
    if (count < 11) {
      throw new OcrAttributeException("属性数据错误，确认图片中包含步兵防御力和弓兵生命值属性信息");
    }

    List<Integer> tail = count > 12 ? values.subList(count - 12, count) : values;

    String[] keys = {
      "步兵攻击力", "步兵防御力", "步兵破坏力", "步兵生命值",
      "骑兵攻击力", "骑兵防御力", "骑兵破坏力", "骑兵生命值",
      "弓兵攻击力", "弓兵防御力", "弓兵破坏力", "弓兵生命值"
    };

    int keyOffset = tail.size() == 11 ? 1 : 0;

    for (int i = 0; i < tail.size(); i++) {
      result.put(keys[i + keyOffset], tail.get(i));
    }

    return result;
  }

  /** 获取本月额度统计 */
  @Transactional
  public List<OcrQuota> getMonthQuotaStats() {
    String currentMonth = java.time.YearMonth.now().toString();
    initMonthQuotas(currentMonth);
    return quotaRepository.findByProviderAndQuotaMonth(PROVIDER_NAME, currentMonth);
  }
}
