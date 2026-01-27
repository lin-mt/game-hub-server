package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新官渡战事人数上限请求")
public class UpdateWarLimitsRequest {

  @NotNull(message = "联盟ID不能为空")
  @Schema(description = "联盟ID", example = "1")
  private Long allianceId;

  @Schema(description = "官渡一主力人员上限", example = "30")
  @Min(value = 1, message = "官渡一主力最小为1")
  @Max(value = 200, message = "官渡一主力最大为200")
  private Integer guanduOneMainLimit;

  @Schema(description = "官渡一替补人员上限", example = "10")
  @Min(value = 0, message = "官渡一替补最小为0")
  @Max(value = 200, message = "官渡一替补最大为200")
  private Integer guanduOneSubLimit;

  @Schema(description = "官渡二主力人员上限", example = "30")
  @Min(value = 1, message = "官渡二主力最小为1")
  @Max(value = 200, message = "官渡二主力最大为200")
  private Integer guanduTwoMainLimit;

  @Schema(description = "官渡二替补人员上限", example = "10")
  @Min(value = 0, message = "官渡二替补最小为0")
  @Max(value = 200, message = "官渡二替补最大为200")
  private Integer guanduTwoSubLimit;
}
