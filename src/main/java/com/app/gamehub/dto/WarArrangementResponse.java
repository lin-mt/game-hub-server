package com.app.gamehub.dto;

import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "战事人员安排响应")
public class WarArrangementResponse {

  @Schema(description = "联盟ID", type = "string")
  private Long allianceId;

  @Schema(description = "战事类型")
  private WarType warType;

  @Schema(description = "机动人员列表")
  private List<GameAccountWithApplicationTime> mobileMembers;

  @Schema(description = "战事分组列表")
  private List<WarGroupDetail> warGroups;

  @Data
  @Schema(description = "战事分组详情")
  public static class WarGroupDetail {

    @Schema(description = "分组ID", type = "string")
    private Long groupId;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "分组任务")
    private String groupTask;

    @Schema(description = "分组成员列表")
    private List<GameAccountWithApplicationTime> members;
  }
}
