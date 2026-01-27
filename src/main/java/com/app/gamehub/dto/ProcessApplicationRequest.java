package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "处理申请请求")
public class ProcessApplicationRequest {

  @NotNull(message = "是否通过不能为空")
  @Schema(description = "是否通过申请")
  private Boolean approved;
}
