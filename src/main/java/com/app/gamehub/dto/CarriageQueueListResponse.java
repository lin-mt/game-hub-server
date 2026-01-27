package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "马车排队列表响应")
public class CarriageQueueListResponse {

  @Schema(description = "联盟ID")
  private Long allianceId;

  @Schema(description = "排队总人数")
  private Integer totalCount;

  @Schema(description = "今日开车的账号ID（如果没有则为null）")
  private Long todayDriverAccountId;

  @Schema(description = "今日开车的账号名称（如果没有则为null）")
  private String todayDriverAccountName;

  @Schema(description = "排队列表")
  private List<CarriageQueueItemResponse> queueList;
}
