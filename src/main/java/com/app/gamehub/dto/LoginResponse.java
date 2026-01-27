package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginResponse {

  @Schema(description = "JWT令牌")
  private String token;

  @Schema(description = "用户ID")
  private String userId;

  @Schema(description = "用户昵称")
  private String nickname;

  @Schema(description = "用户头像URL")
  private String avatarUrl;

  @Schema(description = "是否为新用户")
  private Boolean isNewUser;
}
