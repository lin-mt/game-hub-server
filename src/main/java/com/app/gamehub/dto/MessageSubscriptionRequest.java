package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "消息订阅请求")
public class MessageSubscriptionRequest {

  @NotNull(message = "订阅数量不能为空")
  @Min(value = 1, message = "订阅数量必须大于0")
  @Schema(description = "订阅数量", example = "1")
  private Integer count;
}
