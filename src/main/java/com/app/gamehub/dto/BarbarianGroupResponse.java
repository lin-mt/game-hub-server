package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "南蛮分组响应")
public class BarbarianGroupResponse {

  @Schema(description = "分组ID")
  private String id;

  @Schema(description = "联盟ID")
  private String allianceId;

  @Schema(description = "分组名称")
  private String groupName;

  @Schema(description = "队列数量")
  private Integer queueCount;

  @Schema(description = "创建时间")
  private LocalDateTime createdAt;

  @Schema(description = "更新时间")
  private LocalDateTime updatedAt;
}
