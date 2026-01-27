package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "申请加入联盟请求")
public class JoinAllianceRequest {

  @NotNull(message = "账号ID不能为空")
  @Schema(description = "账号ID")
  private Long accountId;

  @NotBlank(message = "联盟编码不能为空")
  @Schema(description = "联盟编码")
  private String allianceCode;
}
