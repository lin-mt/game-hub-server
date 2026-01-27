package com.app.gamehub.dto;

import com.app.gamehub.entity.PositionReservation;
import com.app.gamehub.entity.PositionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 官职预约结果响应 */
@Data
@Schema(description = "官职预约结果响应")
public class PositionReservationResultResponse {

  @Schema(description = "王朝ID")
  private Long dynastyId;

  @Schema(description = "任职日期")
  private LocalDate dutyDate;

  @Schema(description = "官职预约结果（按官职类型分组）")
  private Map<PositionType, List<PositionReservation>> reservationResults;

  @Schema(description = "可用时段信息（按官职类型分组）")
  private Map<PositionType, List<Integer>> availableTimeSlots;
}
