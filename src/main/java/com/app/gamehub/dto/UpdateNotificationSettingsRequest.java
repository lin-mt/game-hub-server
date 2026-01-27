package com.app.gamehub.dto;

import com.app.gamehub.enums.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "更新通知设置请求")
public class UpdateNotificationSettingsRequest {

  @NotNull(message = "账号ID不能为空")
  @Schema(description = "游戏账号ID", example = "1")
  private Long accountId;

  @Schema(description = "接收的通知类型列表")
  private Set<ActivityType> notificationTypes;
}
