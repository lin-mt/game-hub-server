package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "官渡战事人数上限状态响应")
public class WarLimitStatusResponse {

  @Schema(description = "官渡一主力人员上限")
  private Integer guanduOneMainLimit;

  @Schema(description = "官渡一主力当前已安排人数")
  private Long guanduOneMainArranged;

  @Schema(description = "官渡一主力当前待处理申请人数")
  private Long guanduOneMainPending;

  @Schema(description = "官渡一主力是否已满")
  private Boolean guanduOneMainFull;

  @Schema(description = "官渡一替补人员上限")
  private Integer guanduOneSubLimit;

  @Schema(description = "官渡一替补当前已安排人数")
  private Long guanduOneSubArranged;

  @Schema(description = "官渡一替补当前待处理申请人数")
  private Long guanduOneSubPending;

  @Schema(description = "官渡一替补是否已满")
  private Boolean guanduOneSubFull;

  @Schema(description = "官渡二主力人员上限")
  private Integer guanduTwoMainLimit;

  @Schema(description = "官渡二主力当前已安排人数")
  private Long guanduTwoMainArranged;

  @Schema(description = "官渡二主力当前待处理申请人数")
  private Long guanduTwoMainPending;

  @Schema(description = "官渡二主力是否已满")
  private Boolean guanduTwoMainFull;

  @Schema(description = "官渡二替补人员上限")
  private Integer guanduTwoSubLimit;

  @Schema(description = "官渡二替补当前已安排人数")
  private Long guanduTwoSubArranged;

  @Schema(description = "官渡二替补当前待处理申请人数")
  private Long guanduTwoSubPending;

  @Schema(description = "官渡二替补是否已满")
  private Boolean guanduTwoSubFull;
}
