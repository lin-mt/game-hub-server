package com.app.gamehub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGuanduRegistrationTimeRequest {

  @Min(value = 1, message = "开始星期必须在1-7之间（1=星期一，7=星期日）")
  @Max(value = 7, message = "开始星期必须在1-7之间（1=星期一，7=星期日）")
  private Integer startDay; // 1-7 表示星期一到星期日

  @Min(value = 0, message = "开始分钟必须在0-1439之间")
  @Max(value = 1439, message = "开始分钟必须在0-1439之间")
  private Integer startMinute; // 0-1439 表示一天中的分钟数

  @Min(value = 1, message = "结束星期必须在1-7之间（1=星期一，7=星期日）")
  @Max(value = 7, message = "结束星期必须在1-7之间（1=星期一，7=星期日）")
  private Integer endDay; // 1-7 表示星期一到星期日

  @Min(value = 0, message = "结束分钟必须在0-1439之间")
  @Max(value = 1439, message = "结束分钟必须在0-1439之间")
  private Integer endMinute; // 0-1439 表示一天中的分钟数
}