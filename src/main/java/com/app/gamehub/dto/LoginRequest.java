package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

  @NotBlank(message = "code不能为空")
  @Schema(description = "微信小程序登录凭证")
  private String code;

  @Schema(description = "用户昵称")
  private String nickname;

  @Schema(description = "用户头像URL")
  private String avatarUrl;
}
