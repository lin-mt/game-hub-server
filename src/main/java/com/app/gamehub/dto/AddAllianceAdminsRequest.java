package com.app.gamehub.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class AddAllianceAdminsRequest {

  @NotEmpty(message = "用户ID列表不能为空")
  private List<Long> userIds;
}
