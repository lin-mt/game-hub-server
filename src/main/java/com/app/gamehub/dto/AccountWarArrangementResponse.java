package com.app.gamehub.dto;

import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "账号战事安排响应")
public class AccountWarArrangementResponse {

  @Schema(description = "账号ID", type = "string")
  private String accountId;

  @Schema(description = "账号信息")
  private GameAccount account;

  @Schema(description = "战事安排列表")
  private List<WarArrangementDetail> warArrangements;

  @Data
  @Schema(description = "战事安排详情")
  public static class WarArrangementDetail {

    @Schema(description = "战事类型")
    private WarType warType;

    @Schema(description = "是否为机动人员")
    private Boolean isMobile;

    @Schema(description = "是否为替补人员")
    private Boolean isSubstitute;

    @Schema(description = "战事分组信息（如果不是机动人员）")
    private WarGroupInfo warGroup;

    @Data
    @Schema(description = "战事分组信息")
    public static class WarGroupInfo {

      @Schema(description = "分组ID", type = "string")
      private String groupId;

      @Schema(description = "分组名称")
      private String groupName;

      @Schema(description = "分组任务")
      private String groupTask;

      @Schema(description = "分组成员列表")
      private List<GameAccount> members;
    }
  }
}
