package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新战事分组请求")
public class UpdateWarGroupRequest {

  @Schema(description = "分组名称")
  private String groupName;

  @Schema(description = "分组任务")
  private String groupTask;
}
