package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.dto.UpdateArenaNotificationRequest;
import com.app.gamehub.entity.User;
import com.app.gamehub.service.ArenaNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/arena-notifications")
@Tag(name = "演武场通知管理", description = "演武场通知相关接口")
@RequiredArgsConstructor
public class ArenaNotificationController {

  private final ArenaNotificationService arenaNotificationService;

  @PutMapping("/settings")
  @Operation(summary = "更新演武场通知设置")
  public ApiResponse<User> updateArenaNotificationSetting(
      @Valid @RequestBody UpdateArenaNotificationRequest request) {
    User user = arenaNotificationService.updateArenaNotificationSetting(request);
    return ApiResponse.success(user);
  }

  @GetMapping("/settings")
  @Operation(summary = "获取演武场通知设置")
  public ApiResponse<Boolean> getArenaNotificationSetting() {
    Boolean enabled = arenaNotificationService.getArenaNotificationSetting();
    return ApiResponse.success(enabled);
  }
}
