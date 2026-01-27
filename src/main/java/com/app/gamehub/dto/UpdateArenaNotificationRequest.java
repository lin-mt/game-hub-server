package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新演武场通知设置请求")
public class UpdateArenaNotificationRequest {

  @NotNull(message = "通知设置不能为空")
  @Schema(description = "是否接收演武场通知", example = "true")
  private Boolean enabled;
}
