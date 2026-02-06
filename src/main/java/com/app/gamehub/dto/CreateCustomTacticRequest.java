package com.app.gamehub.dto;

import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class CreateCustomTacticRequest {

  @NotNull(message = "联盟ID不能为空")
  @Schema(description = "联盟ID", example = "1")
  private Long allianceId;

  @NotNull(message = "战事类型不能为空")
  @Schema(description = "战事类型", example = "GUANDU_ONE")
  private WarType warType;

  @NotBlank(message = "战术名称不能为空")
  @Schema(description = "战术名称", example = "官渡自定义战术A")
  private String name;

  @Schema(description = "战术分组配置")
  private List<CustomTacticConfig.Group> groups;
}
