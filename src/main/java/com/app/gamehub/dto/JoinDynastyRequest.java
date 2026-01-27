package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 加入王朝请求 */
@Data
@Schema(description = "加入王朝请求")
public class JoinDynastyRequest {

  @NotBlank(message = "王朝编码不能为空")
  @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "王朝编码必须是6位英文数字组合")
  @Schema(description = "王朝编码", example = "ABC123")
  private String dynastyCode;
}
