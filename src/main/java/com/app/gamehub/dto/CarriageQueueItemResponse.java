package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "马车排队项")
public class CarriageQueueItemResponse {

  @Schema(description = "排队记录ID")
  private Long id;

  @Schema(description = "游戏账号ID")
  private Long accountId;

  @Schema(description = "游戏账号名称")
  private String accountName;

  @Schema(description = "排队顺序（从1开始）")
  private Integer queueOrder;

  @Schema(description = "是否是今日开车的人")
  private Boolean isTodayDriver;

  @Schema(description = "最后开车日期")
  private LocalDate lastDriveDate;

  @Schema(description = "报名时间")
  private LocalDateTime createdAt;
}
