package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "转交联盟请求")
public class TransferAllianceRequest {

  @NotNull(message = "新盟主账户ID不能为空")
  @Schema(description = "新盟主账户ID")
  private Long accountId;
}
