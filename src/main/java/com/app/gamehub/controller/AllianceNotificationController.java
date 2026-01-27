package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.dto.SendAllianceNotificationRequest;
import com.app.gamehub.dto.UpdateNotificationSettingsRequest;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.service.AllianceNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alliance-notifications")
@Tag(name = "联盟通知管理", description = "联盟活动通知相关接口")
public class AllianceNotificationController {

  private final AllianceNotificationService allianceNotificationService;

  public AllianceNotificationController(AllianceNotificationService allianceNotificationService) {
    this.allianceNotificationService = allianceNotificationService;
  }

  @PostMapping("/send")
  @Operation(summary = "发送联盟活动通知")
  public ApiResponse<Void> sendAllianceNotification(@Valid @RequestBody SendAllianceNotificationRequest request) {
    allianceNotificationService.sendAllianceNotification(request);
    return ApiResponse.success("联盟活动通知发送成功", null);
  }

  @PutMapping("/settings")
  @Operation(summary = "更新账号通知设置")
  public ApiResponse<GameAccount> updateNotificationSettings(@Valid @RequestBody UpdateNotificationSettingsRequest request) {
    GameAccount account = allianceNotificationService.updateNotificationSettings(request);
    return ApiResponse.success("通知设置更新成功", account);
  }
}
