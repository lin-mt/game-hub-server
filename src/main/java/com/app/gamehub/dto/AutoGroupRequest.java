package com.app.gamehub.dto;

import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "自动分组请求")
public class AutoGroupRequest {

  @NotNull(message = "联盟ID不能为空")
  @Schema(description = "联盟ID")
  private Long allianceId;

  @NotNull(message = "战事类型不能为空")
  @Schema(description = "战事类型")
  private WarType warType;

  @NotNull(message = "总车头数不能为空")
  @Min(value = 1, message = "总车头数最少为1")
  @Max(value = 20, message = "总车头数最多为20")
  @Schema(description = "总车头数（1～20）")
  private Integer totalLeaders;

  @Schema(description = "车身分配方式（AVERAGE-平均分配, LEADER_PRIORITY-大车头优先），默认AVERAGE")
  private AllocationMethod allocationMethod;

  @NotEmpty(message = "分波配置不能为空")
  @Valid
  @Schema(description = "分波配置列表")
  private List<WaveConfig> waves;

  @Data
  @Schema(description = "分波配置")
  public static class WaveConfig {

    @Schema(description = "波次序号（从1开始）")
    private Integer waveIndex;

    @Min(value = 1, message = "每波车头数最少为1")
    @Schema(description = "该波的车头数")
    private Integer leaderCount;
  }

  @Schema(description = "车身分配方式")
  public enum AllocationMethod {
    AVERAGE,
    LEADER_PRIORITY
  }
}
