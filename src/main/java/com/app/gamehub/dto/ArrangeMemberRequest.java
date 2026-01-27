package com.app.gamehub.dto;

import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "安排成员请求")
public class ArrangeMemberRequest {

  @NotNull(message = "账号ID不能为空")
  @Schema(description = "账号ID")
  private Long accountId;

  @NotNull(message = "战事类型不能为空")
  @Schema(description = "战事类型")
  private WarType warType;

  @Schema(description = "战事分组ID（为空表示机动人员）")
  private Long warGroupId;
}
