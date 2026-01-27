package com.app.gamehub.dto;

import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "清空战事安排请求")
public class ClearWarArrangementsRequest {

  @NotNull(message = "联盟ID不能为空")
  @Schema(description = "联盟ID", example = "1")
  private Long allianceId;

  @NotNull(message = "战事类型不能为空")
  @Schema(description = "战事类型")
  private WarType warType;

  @Schema(description = "是否发送官渡报名通知", example = "false")
  private Boolean sendNotification = false;

  @Schema(description = "通知备注（发送通知时使用）", example = "官渡战事重新开放报名，请及时申请参加")
  private String notificationRemark;
}
