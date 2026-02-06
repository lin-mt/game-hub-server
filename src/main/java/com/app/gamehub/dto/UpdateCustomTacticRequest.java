package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class UpdateCustomTacticRequest {

  @NotBlank(message = "战术名称不能为空")
  @Schema(description = "战术名称", example = "官渡自定义战术A")
  private String name;

  @Schema(description = "战术分组配置")
  private List<CustomTacticConfig.Group> groups;
}
