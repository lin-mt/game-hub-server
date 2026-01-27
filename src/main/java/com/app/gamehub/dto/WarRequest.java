package com.app.gamehub.dto;

import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "战事请求")
public class WarRequest {

  @NotNull(message = "账号ID不能为空")
  @Schema(description = "账号ID")
  private Long accountId;

  @NotNull(message = "战事类型不能为空")
  @Schema(description = "战事类型")
  private WarType warType;

  @Schema(description = "是否为替补人员，默认false（正式人员）")
  private Boolean isSubstitute = false;
}
