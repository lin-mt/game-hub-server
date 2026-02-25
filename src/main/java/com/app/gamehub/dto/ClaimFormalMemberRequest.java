package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "认领联盟正式成员账号请求")
public class ClaimFormalMemberRequest {

  @NotNull(message = "正式成员账号ID不能为空")
  @Schema(description = "联盟正式成员账号ID（必须是未认领账号）")
  private Long sourceAccountId;
}
