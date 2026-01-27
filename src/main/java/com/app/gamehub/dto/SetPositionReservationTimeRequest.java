package com.app.gamehub.dto;

import com.app.gamehub.entity.PositionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 设置官职预约时间请求 */
@Data
@Schema(description = "设置官职预约时间请求")
public class SetPositionReservationTimeRequest {

  @NotNull(message = "官职类型不能为空")
  @Schema(description = "官职类型", example = "TAI_WEI")
  private PositionType positionType;

  @NotNull(message = "预约开始时间不能为空")
  @Schema(description = "预约开始时间", example = "2025/08/11 10:00:00")
  private LocalDateTime reservationStartTime;

  @NotNull(message = "预约结束时间不能为空")
  @Schema(description = "预约结束时间", example = "2025/08/11 23:00:00")
  private LocalDateTime reservationEndTime;

  @NotNull(message = "任职日期不能为空")
  @Schema(description = "任职日期", example = "2025/08/12")
  private LocalDate dutyDate;

  @Schema(description = "禁用的时段列表（0-23）", example = "[0, 1, 2]")
  private List<Integer> disabledTimeSlots;
}
