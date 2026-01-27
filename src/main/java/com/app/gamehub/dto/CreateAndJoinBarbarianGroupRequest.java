package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建并加入南蛮分组请求")
public class CreateAndJoinBarbarianGroupRequest {

  @NotNull(message = "联盟ID不能为空")
  @Schema(description = "联盟ID", example = "1")
  private Long allianceId;

  @NotBlank(message = "分组名称不能为空")
  @Schema(description = "分组名称", example = "第一小队")
  private String groupName;

  @NotNull(message = "队列数量不能为空")
  @Min(value = 1, message = "队列数量最小为1")
  @Max(value = 6, message = "队列数量最大为6")
  @Schema(description = "队列数量", example = "3")
  private Integer queueCount;

  @NotNull(message = "账号ID不能为空")
  @Schema(description = "要加入分组的账号ID", example = "1")
  private Long accountId;
}
