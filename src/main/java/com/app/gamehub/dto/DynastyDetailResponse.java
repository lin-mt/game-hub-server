package com.app.gamehub.dto;

import com.app.gamehub.entity.Dynasty;
import com.app.gamehub.entity.DynastyPosition;
import com.app.gamehub.entity.PositionReservation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 王朝详情响应 */
@Data
@Schema(description = "王朝详情响应")
public class DynastyDetailResponse {

  @Schema(description = "王朝基本信息")
  private Dynasty dynasty;

  @Schema(description = "官职配置列表")
  private List<DynastyPosition> positions;

  @Schema(description = "官职预约结果（按官职类型分组）")
  private Map<String, List<PositionReservation>> positionReservations;
}
