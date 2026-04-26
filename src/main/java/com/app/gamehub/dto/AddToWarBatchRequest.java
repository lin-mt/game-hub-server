package com.app.gamehub.dto;

import com.app.gamehub.entity.WarType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "批量添加成员到战事请求")
public class AddToWarBatchRequest {

  @NotEmpty(message = "账号ID列表不能为空")
  @Schema(description = "账号ID列表")
  private List<Long> accountIds;

  @NotNull(message = "战事类型不能为空")
  @Schema(description = "战事类型")
  private WarType warType;

  @Schema(description = "是否为替补人员，默认 false")
  private Boolean isSubstitute = false;
}
