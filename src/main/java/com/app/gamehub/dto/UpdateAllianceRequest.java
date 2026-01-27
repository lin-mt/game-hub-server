package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新联盟请求")
public class UpdateAllianceRequest {

  @Schema(description = "联盟名称")
  private String name;

  @Schema(description = "联盟编码")
  private String code;

  @Schema(description = "官渡提醒信息")
  private String guanduReminder;

  @Schema(description = "YY语音频道ID")
  private String yyChannelId;

  @Schema(description = "腾讯会议码")
  private String tencentMeetingCode;

  @Schema(description = "腾讯会议密码")
  private String tencentMeetingPassword;
}
