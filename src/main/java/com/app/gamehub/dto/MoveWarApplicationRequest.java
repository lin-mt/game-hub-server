package com.app.gamehub.dto;

import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "移动战事申请请求")
public class MoveWarApplicationRequest {

  @NotNull(message = "新战事类型不能为空")
  @Schema(description = "新战事类型")
  private WarType newWarType;
}
