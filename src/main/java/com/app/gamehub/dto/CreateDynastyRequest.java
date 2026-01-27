package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 创建王朝请求 */
@Data
@Schema(description = "创建王朝请求")
public class CreateDynastyRequest {

  @NotBlank(message = "王朝名称不能为空")
  @Schema(description = "王朝名称", example = "大汉王朝")
  private String name;

  @NotNull(message = "区号不能为空")
  @Positive(message = "区号必须为正整数")
  @Schema(description = "区号", example = "1")
  private Integer serverId;
}
