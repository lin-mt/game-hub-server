package com.app.gamehub.dto;

import com.app.gamehub.enums.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "发送联盟活动通知请求")
public class SendAllianceNotificationRequest {

  @NotNull(message = "联盟ID不能为空")
  @Schema(description = "联盟ID", example = "1")
  private Long allianceId;

  @NotNull(message = "活动类型不能为空")
  @Schema(description = "活动类型")
  private ActivityType activityType;

  @NotNull(message = "开始时间不能为空")
  @Schema(description = "活动开始时间", example = "2024/01/01 20:00:00")
  private LocalDateTime startTime;

  @Size(max = 200, message = "备注内容不能超过200个字符")
  @Schema(description = "备注内容", example = "活动时间为活动预计开启时间，如有变更，盟主将另行通知")
  private String remark;
}
