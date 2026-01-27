package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.entity.OcrQuota;
import com.app.gamehub.repository.OcrQuotaRepository;
import com.app.gamehub.service.ocr.BaiduOcrServiceImpl;
import com.app.gamehub.service.ocr.OcrServiceManager;
import com.app.gamehub.service.ocr.TencentOcrServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCR额度管理控制器（多服务商）
 */
@RestController
@RequestMapping("/api/ocr-quota")
@Tag(name = "OCR额度管理", description = "查询多个OCR服务商的额度使用情况")
public class OcrQuotaController {

  @Autowired private OcrQuotaRepository quotaRepository;
  @Autowired private BaiduOcrServiceImpl baiduOcrService;
  @Autowired private TencentOcrServiceImpl tencentOcrService;
  @Autowired private OcrServiceManager ocrServiceManager;

  @GetMapping("/month")
  @Operation(summary = "获取本月所有服务商额度统计", description = "查询本月所有OCR服务商的额度使用情况")
  public ApiResponse<List<OcrQuota>> getAllMonthQuotaStats() {
    String currentMonth = java.time.YearMonth.now().toString();
    List<OcrQuota> quotas = quotaRepository.findAllByQuotaMonth(currentMonth);
    return ApiResponse.success(quotas);
  }

  @GetMapping("/month/{provider}")
  @Operation(summary = "获取指定服务商本月额度统计", description = "查询指定服务商本月的额度使用情况")
  public ApiResponse<List<OcrQuota>> getProviderMonthQuotaStats(@PathVariable String provider) {
    List<OcrQuota> quotas;
    
    switch (provider.toLowerCase()) {
      case "baidu":
        quotas = baiduOcrService.getMonthQuotaStats();
        break;
      case "tencent":
        quotas = tencentOcrService.getMonthQuotaStats();
        break;
      default:
        String currentMonth = java.time.YearMonth.now().toString();
        quotas = quotaRepository.findByProviderAndQuotaMonth(provider, currentMonth);
        break;
    }
    
    return ApiResponse.success(quotas);
  }

  @GetMapping("/services")
  @Operation(summary = "获取所有OCR服务商状态", description = "查询所有OCR服务商的可用状态和优先级")
  public ApiResponse<List<OcrServiceManager.OcrServiceInfo>> getOcrServices() {
    List<OcrServiceManager.OcrServiceInfo> services = ocrServiceManager.getAvailableServices();
    return ApiResponse.success(services);
  }

  @GetMapping("/round-robin")
  @Operation(summary = "获取轮询统计信息", description = "查询OCR服务轮询调用的统计信息")
  public ApiResponse<OcrServiceManager.RoundRobinStats> getRoundRobinStats() {
    OcrServiceManager.RoundRobinStats stats = ocrServiceManager.getRoundRobinStats();
    return ApiResponse.success(stats);
  }
}