package com.app.gamehub.dto;

import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建战事分组请求")
public class CreateWarGroupRequest {

  @NotNull(message = "联盟ID不能为空")
  @Schema(description = "联盟ID")
  private Long allianceId;

  @NotNull(message = "战事类型不能为空")
  @Schema(description = "战事类型")
  private WarType warType;

  @NotBlank(message = "分组名称不能为空")
  @Schema(description = "分组名称")
  private String groupName;

  @Schema(description = "分组任务")
  private String groupTask;
}
