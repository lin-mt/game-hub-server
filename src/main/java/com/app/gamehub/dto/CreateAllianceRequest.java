package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "创建联盟请求")
public class CreateAllianceRequest {

  @NotBlank(message = "联盟名称不能为空")
  @Schema(description = "联盟名称")
  private String name;

  @NotNull(message = "区号不能为空")
  @Positive(message = "区号必须为正整数")
  @Schema(description = "区号")
  private Integer serverId;

  @Schema(description = "申请加入联盟是否需要审核")
  private Boolean allianceJoinApprovalRequired = false;

  @Schema(description = "报名官渡是否需要审核")
  private Boolean warJoinApprovalRequired = false;

  @Positive(message = "官渡一主力人员上限必须为正整数")
  @Schema(description = "官渡一主力人员上限")
  private Integer guanduOneMainLimit = 30;

  @Positive(message = "官渡一替补人员上限必须为非负整数")
  @Schema(description = "官渡一替补人员上限")
  private Integer guanduOneSubLimit = 10;

  @Positive(message = "官渡二主力人员上限必须为正整数")
  @Schema(description = "官渡二主力人员上限")
  private Integer guanduTwoMainLimit = 30;

  @Positive(message = "官渡二替补人员上限必须为非负整数")
  @Schema(description = "官渡二替补人员上限")
  private Integer guanduTwoSubLimit = 10;

  @Schema(description = "官渡提醒信息")
  private String guanduReminder;

  @Schema(description = "YY语音频道ID")
  private String yyChannelId;

  @Schema(description = "腾讯会议码")
  private String tencentMeetingCode;

  @Schema(description = "腾讯会议密码")
  private String tencentMeetingPassword;

  @Schema(description = "联盟马车时间")
  private String carriageTime;
}
