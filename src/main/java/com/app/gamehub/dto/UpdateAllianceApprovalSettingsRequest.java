package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新联盟审核设置请求")
public class UpdateAllianceApprovalSettingsRequest {

  @NotNull(message = "联盟ID不能为空")
  @Schema(description = "联盟ID", example = "1")
  private Long allianceId;

  @Schema(description = "加入联盟是否需要审核", example = "true")
  private Boolean allianceJoinApprovalRequired;

  @Schema(description = "申请参加官渡是否需要审核", example = "true")
  private Boolean warJoinApprovalRequired;
}
