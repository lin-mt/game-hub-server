package com.app.gamehub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddAllianceAdminRequest {
  @NotNull(message = "用户ID不能为空")
  private Long userId;
}