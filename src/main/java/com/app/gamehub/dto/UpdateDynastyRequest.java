package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 更新王朝请求 */
@Data
@Schema(description = "更新王朝请求")
public class UpdateDynastyRequest {

  @NotBlank(message = "王朝名称不能为空")
  @Schema(description = "王朝名称", example = "大汉王朝")
  private String name;
}
