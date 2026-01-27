package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "加入南蛮分组请求")
public class JoinBarbarianGroupRequest {

  @NotNull(message = "分组ID不能为空")
  @Schema(description = "分组ID", example = "1")
  private Long groupId;

  @NotNull(message = "账号ID不能为空")
  @Schema(description = "账号ID", example = "1")
  private Long accountId;
}
