package com.app.gamehub.dto;

import com.app.gamehub.entity.PositionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 预约官职请求 */
@Data
@Schema(description = "预约/取消官职请求")
public class ReservePositionRequest {

  @NotNull(message = "官职类型不能为空")
  @Schema(description = "官职类型", example = "TAI_WEI")
  private PositionType positionType;

  @NotNull(message = "时段不能为空")
  @Min(value = 0, message = "时段必须在0-23之间")
  @Max(value = 23, message = "时段必须在0-23之间")
  @Schema(description = "时段（0-23，表示00:00-00:59到23:00-23:59）", example = "10")
  private Integer timeSlot;
}
