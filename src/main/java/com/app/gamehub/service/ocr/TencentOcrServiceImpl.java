package com.app.gamehub.service.ocr;

import com.app.gamehub.entity.OcrQuota;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.OcrQuotaRepository;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.AdvertiseOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.AdvertiseOCRResponse;
import com.tencentcloudapi.ocr.v20181119.models.AdvertiseTextDetection;
import com.tencentcloudapi.ocr.v20181119.models.GeneralAccurateOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.GeneralAccurateOCRResponse;
import com.tencentcloudapi.ocr.v20181119.models.GeneralBasicOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.GeneralBasicOCRResponse;
import com.tencentcloudapi.ocr.v20181119.models.TableOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.TableOCRResponse;
import com.tencentcloudapi.ocr.v20181119.models.TextTable;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 腾讯云OCR服务实现 */
@Slf4j
@Service("tencentOcrService")
public class TencentOcrServiceImpl implements OcrService {

  private static final String PROVIDER_NAME = "tencent";
  private static final String SECRET_ID = "";
  private static final String SECRET_KEY = "";

  // QPS限制：腾讯云OCR默认QPS限制
  private static final long QPS_INTERVAL_MS = 100; // 10 QPS

  private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d{1,4})(?:\\.\\d+)?%");

  @Autowired private OcrQuotaRepository quotaRepository;

  @Override
  @Transactional
  public Map<String, Integer> parseStats(File imageFile) throws Exception {
    // 检查密钥配置
    if (SECRET_ID == null || SECRET_KEY == null) {
      throw new BusinessException("腾讯云OCR密钥未配置");
    }

    String currentMonth = java.time.YearMonth.now().toString();

    // 初始化本月额度记录
    initMonthQuotas(currentMonth);

    // 裁剪图片左半部分
    File croppedFile = null;
    try {
      croppedFile = com.app.gamehub.util.ImageUtils.cropLeftHalf(imageFile);
      log.debug("腾讯云OCR图片裁剪成功，原始文件: {}, 裁剪后文件: {}", imageFile.getName(), croppedFile.getName());

      // 获取可用的服务列表
      List<OcrQuota> availableQuotas =
          quotaRepository.findAvailableQuotasByProviderAndMonth(PROVIDER_NAME, currentMonth);

      if (availableQuotas.isEmpty()) {
        throw new BusinessException("腾讯云OCR服务的免费额度已用完");
      }

      // 尝试使用每个可用的服务
      Exception lastException = null;
      for (OcrQuota quota : availableQuotas) {
        try {
          log.info(
              "尝试使用腾讯云OCR服务: {} (剩余额度: {}/{})",
              quota.getServiceType(),
              quota.getRemainingQuota(),
              quota.getTotalQuota());

          // 执行OCR识别
          Map<String, Integer> result = performOcr(croppedFile, quota);

          // 使用数据库原子操作更新额度
          updateQuotaAtomic(quota.getId());

          log.info("腾讯云OCR识别成功，使用服务: {}", quota.getServiceType());
          return result;

        } catch (BusinessException be) {
          lastException = be;
          log.warn("腾讯云OCR服务 {} 识别失败: {}", quota.getServiceType(), be.getMessage());

          if (be.getMessage().contains("QPS") || be.getMessage().contains("限流")) {
            continue;
          }
          if (be.getMessage().contains("额度") || be.getMessage().contains("quota")) {
            markQuotaExhausted(quota.getId());
          }
        } catch (Exception e) {
          lastException = e;
          log.warn("腾讯云OCR服务 {} 调用异常: {}", quota.getServiceType(), e.getMessage());
        }
      }

      throw new BusinessException("腾讯云OCR所有服务都不可用: " + lastException.getMessage());
    } finally {
      // 清理裁剪后的临时文件
      if (croppedFile != null && croppedFile.exists()) {
        try {
          Files.deleteIfExists(croppedFile.toPath());
          log.debug("已删除腾讯云OCR裁剪后的临时文件: {}", croppedFile.getName());
        } catch (Exception e) {
          log.warn("删除腾讯云OCR裁剪后的临时文件失败: {}", e.getMessage());
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
      // 检查密钥配置
      if (SECRET_ID == null || SECRET_KEY == null) {
        return false;
      }

      String currentMonth = java.time.YearMonth.now().toString();

      // 先初始化本月额度记录（如果不存在）
      initMonthQuotas(currentMonth);

      List<OcrQuota> availableQuotas =
          quotaRepository.findAvailableQuotasByProviderAndMonth(PROVIDER_NAME, currentMonth);
      return !availableQuotas.isEmpty();
    } catch (Exception e) {
      log.error("检查腾讯云OCR可用性失败", e);
      return false;
    }
  }

  /** 执行OCR识别 */
  private Map<String, Integer> performOcr(File imageFile, OcrQuota quota) throws Exception {
    TencentOcrServiceType serviceType = getServiceTypeByCode(quota.getServiceType());

    // QPS限流
    enforceQpsLimit(quota);

    // 读取图片文件并转换为Base64
    byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
    String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

    try {
      // 创建认证对象
      Credential cred = new Credential(SECRET_ID, SECRET_KEY);

      // 实例化HTTP选项
      HttpProfile httpProfile = new HttpProfile();
      httpProfile.setEndpoint("ocr.tencentcloudapi.com");

      // 实例化客户端选项
      ClientProfile clientProfile = new ClientProfile();
      clientProfile.setHttpProfile(httpProfile);

      // 实例化OCR客户端
      OcrClient client = new OcrClient(cred, "", clientProfile);

      String responseText;

      // 根据服务类型调用不同的OCR接口
      switch (serviceType) {
        case GENERAL_BASIC:
          GeneralBasicOCRRequest basicReq = new GeneralBasicOCRRequest();
          basicReq.setImageBase64(imageBase64);
          GeneralBasicOCRResponse basicResp = client.GeneralBasicOCR(basicReq);
          responseText = extractTextFromResponse(basicResp.getTextDetections());
          break;

        case GENERAL_ACCURATE:
          GeneralAccurateOCRRequest accurateReq = new GeneralAccurateOCRRequest();
          accurateReq.setImageBase64(imageBase64);
          GeneralAccurateOCRResponse accurateResp = client.GeneralAccurateOCR(accurateReq);
          responseText = extractTextFromResponse(accurateResp.getTextDetections());
          break;

        case TABLE_OCR:
          TableOCRRequest tableReq = new TableOCRRequest();
          tableReq.setImageBase64(imageBase64);
          TableOCRResponse tableResp = client.TableOCR(tableReq);
          responseText = extractTextFromResponse(tableResp.getTextDetections());
          break;

        case ADVERTISE_OCR:
        default:
          AdvertiseOCRRequest advertiseReq = new AdvertiseOCRRequest();
          advertiseReq.setImageBase64(imageBase64);
          AdvertiseOCRResponse advertiseResp = client.AdvertiseOCR(advertiseReq);
          responseText = extractTextFromResponse(advertiseResp.getTextDetections());
          break;
      }

      log.info("腾讯云OCR识别成功，服务类型: {}, 识别文本长度: {}", serviceType.getName(), responseText.length());

      // 解析属性数据
      return extractStats(responseText);

    } catch (TencentCloudSDKException e) {
      log.error("腾讯云OCR API调用失败: {}", e.getMessage());

      // 检查是否是额度用完的错误
      if (e.getMessage().contains("quota")
          || e.getMessage().contains("limit")
          || e.getMessage().contains("exceed")) {
        throw new BusinessException("腾讯云OCR服务 " + serviceType.getName() + " 额度已用完");
      }

      // 检查是否是QPS限制错误
      if (e.getMessage().contains("rate") || e.getMessage().contains("throttle")) {
        throw new BusinessException("腾讯云OCR服务 " + serviceType.getName() + " QPS限制");
      }

      throw new BusinessException("腾讯云OCR识别失败: " + e.getMessage());
    }
  }

  private String extractTextFromResponse(AdvertiseTextDetection[] detections) {
    if (detections == null || detections.length == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (AdvertiseTextDetection detection : detections) {
      if (StringUtils.isNotEmpty(detection.getDetectedText())) {
        sb.append(detection.getDetectedText()).append(" ");
      }
    }

    return sb.toString().trim();
  }

  private String extractTextFromResponse(TextTable[] detections) {
    if (detections == null || detections.length == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (TextTable detection : detections) {
      if (StringUtils.isNotEmpty(detection.getText())) {
        sb.append(detection.getText()).append(" ");
      }
    }

    return sb.toString().trim();
  }

  /** QPS限流 */
  private synchronized void enforceQpsLimit(OcrQuota quota) {
    long now = System.currentTimeMillis();
    long lastTime = quota.getLastRequestTime();
    long elapsed = now - lastTime;

    if (elapsed < QPS_INTERVAL_MS) {
      try {
        long sleepTime = QPS_INTERVAL_MS - elapsed;
        log.debug("腾讯云OCR服务 {} QPS限流，等待{}ms", quota.getServiceType(), sleepTime);
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
        log.debug("腾讯云OCR原子更新额度统计成功，ID: {}", quotaId);
      } else {
        log.warn("腾讯云OCR原子更新额度统计失败（可能额度已用完），ID: {}", quotaId);
      }
    } catch (Exception e) {
      log.error("腾讯云OCR原子更新额度统计异常", e);
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
      log.info("标记腾讯云OCR服务 {} 额度已用完", quota.getServiceType());
    } catch (Exception e) {
      log.error("标记腾讯云OCR额度失败", e);
    }
  }

  /** 初始化本月额度记录 */
  @Transactional
  public void initMonthQuotas(String month) {
    for (TencentOcrServiceType serviceType : TencentOcrServiceType.values()) {
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
        log.info("初始化腾讯云OCR服务 {} 的本月额度: {}", serviceType.getName(), serviceType.getMonthlyQuota());
      }
    }
  }

  /** 获取服务类型枚举 */
  private TencentOcrServiceType getServiceTypeByCode(String code) {
    for (TencentOcrServiceType type : TencentOcrServiceType.values()) {
      if (type.getCode().equals(code)) {
        return type;
      }
    }
    return TencentOcrServiceType.ADVERTISE_OCR;
  }

  /** 从腾讯云OCR响应中提取文本 */
  private String extractTextFromResponse(
      com.tencentcloudapi.ocr.v20181119.models.TextDetection[] textDetections) {
    if (textDetections == null || textDetections.length == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (com.tencentcloudapi.ocr.v20181119.models.TextDetection detection : textDetections) {
      if (detection.getDetectedText() != null) {
        sb.append(detection.getDetectedText()).append(" ");
      }
    }

    return sb.toString().trim();
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
      throw new BusinessException("属性数据错误，应该至少包含11个属性");
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

  /** 腾讯云OCR服务类型枚举 */
  @Getter
  public enum TencentOcrServiceType {
    GENERAL_BASIC("general_basic", "通用印刷体识别", 1000),
    GENERAL_ACCURATE("general_accurate", "通用文字识别（高精度版）", 1000),
    TABLE_OCR("table_ocr", "表格识别（V3）", 1000),
    ADVERTISE_OCR("advertise_ocr", "广告文字识别", 1000);

    private final String code;
    private final String name;
    private final int monthlyQuota;

    TencentOcrServiceType(String code, String name, int monthlyQuota) {
      this.code = code;
      this.name = name;
      this.monthlyQuota = monthlyQuota;
    }
  }
}
