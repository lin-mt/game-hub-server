package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "报名马车请求")
public class JoinCarriageQueueRequest {

  @NotNull(message = "联盟ID不能为空")
  @Schema(description = "联盟ID", example = "1")
  private Long allianceId;

  @NotNull(message = "游戏账号ID不能为空")
  @Schema(description = "游戏账号ID", example = "1")
  private Long accountId;
}
