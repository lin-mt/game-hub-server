package com.app.gamehub.service.ocr;

import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.exception.OcrAttributeException;
import com.app.gamehub.util.GameStatOcrUtil;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** OCR服务管理器 统一管理多个OCR服务商，使用轮询策略均匀分配负载 */
@Slf4j
@Service
public class OcrServiceManager {

  @Autowired private List<OcrService> ocrServices;

  // 轮询计数器，用于实现轮询策略
  private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

  /**
   * 使用OCR识别图片中的属性信息 使用轮询策略选择可用服务，最后降级到本地OCR
   *
   * @param imageFile 图片文件
   * @return 属性Map
   */
  public Map<String, Integer> parseStats(File imageFile) {
    // 获取所有可用的OCR服务
    List<OcrService> availableServices =
        ocrServices.stream().filter(OcrService::isAvailable).toList();

    if (availableServices.isEmpty()) {
      log.warn("没有可用的云端OCR服务，直接降级到本地OCR");
      return fallbackToLocalOcr(imageFile);
    }

    log.info("开始OCR识别，可用服务商数量: {}", availableServices.size());

    if (availableServices.size() > 1) {
      return new HashMap<>();
    }

    // 使用轮询策略选择服务
    Exception lastException = null;
    int attempts = 0;
    int maxAttempts = availableServices.size();

    while (attempts < maxAttempts) {
      // 计算当前轮询位置
      int currentIndex = roundRobinCounter.getAndIncrement() % availableServices.size();
      OcrService service = availableServices.get(currentIndex);

      try {
        // 再次检查服务可用性（可能在获取列表后状态发生变化）
        if (!service.isAvailable()) {
          log.info("OCR服务商 {} 当前不可用，跳过", service.getProviderName());
          attempts++;
          continue;
        }

        log.info(
            "轮询选择OCR服务商: {} (轮询位置: {}/{})",
            service.getProviderName(),
            currentIndex + 1,
            availableServices.size());

        Map<String, Integer> result = service.parseStats(imageFile);

        log.info("OCR识别成功，使用服务商: {}", service.getProviderName());
        return result;

      } catch (OcrAttributeException oae) {
        // 属性数据错误，不可挽回，直接抛出
        log.error("OCR服务商 {} 属性数据错误，直接抛出异常", service.getProviderName(), oae);
        throw oae;
      } catch (BusinessException be) {
        lastException = be;
        log.warn("OCR服务商 {} 识别失败: {}", service.getProviderName(), be.getMessage());
        attempts++;
      } catch (Exception e) {
        lastException = e;
        log.warn("OCR服务商 {} 调用异常: {}", service.getProviderName(), e.getMessage());
        attempts++;
      }
    }

    // 所有云端OCR服务都失败，降级到本地OCR
    log.warn("所有云端OCR服务都不可用，降级到本地OCR");
    return fallbackToLocalOcr(imageFile, lastException);
  }

  /** 降级到本地OCR */
  private Map<String, Integer> fallbackToLocalOcr(File imageFile) {
    return fallbackToLocalOcr(imageFile, null);
  }

  /** 降级到本地OCR */
  private Map<String, Integer> fallbackToLocalOcr(File imageFile, Exception lastException) {
    try {
      Map<String, Integer> result = GameStatOcrUtil.parseStats(imageFile);
      log.info("本地OCR识别成功");
      return result;
    } catch (Exception e) {
      log.error("本地OCR识别也失败", e);
      throw new BusinessException(
          "所有OCR服务都不可用: " + (lastException != null ? lastException.getMessage() : "未知错误"));
    }
  }

  /**
   * 获取所有OCR服务商信息（按服务商名称排序）
   *
   * @return 服务商信息列表
   */
  public List<OcrServiceInfo> getAvailableServices() {
    return ocrServices.stream()
        .map(service -> new OcrServiceInfo(service.getProviderName(), service.isAvailable()))
        .sorted(Comparator.comparing(OcrServiceInfo::getProviderName))
        .collect(Collectors.toList());
  }

  /**
   * 获取轮询统计信息
   *
   * @return 轮询统计
   */
  public RoundRobinStats getRoundRobinStats() {
    List<OcrService> availableServices =
        ocrServices.stream().filter(OcrService::isAvailable).toList();

    int currentPosition =
        availableServices.isEmpty() ? 0 : roundRobinCounter.get() % availableServices.size();

    String nextService =
        availableServices.isEmpty()
            ? "无可用服务"
            : availableServices.get(currentPosition).getProviderName();

    return new RoundRobinStats(
        roundRobinCounter.get(), availableServices.size(), currentPosition, nextService);
  }

  /** OCR服务信息 */
  @Getter
  public static class OcrServiceInfo {
    private final String providerName;
    private final boolean available;

    public OcrServiceInfo(String providerName, boolean available) {
      this.providerName = providerName;
      this.available = available;
    }
  }

  /** 轮询统计信息 */
  @Getter
  public static class RoundRobinStats {
    private final int totalCalls;
    private final int availableServices;
    private final int currentPosition;
    private final String nextService;

    public RoundRobinStats(
        int totalCalls, int availableServices, int currentPosition, String nextService) {
      this.totalCalls = totalCalls;
      this.availableServices = availableServices;
      this.currentPosition = currentPosition;
      this.nextService = nextService;
    }
  }
}
