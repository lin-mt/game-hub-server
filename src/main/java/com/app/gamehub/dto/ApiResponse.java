package com.app.gamehub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "API响应")
public class ApiResponse<T> {

  @Schema(description = "响应码")
  private Integer code;

  @Schema(description = "响应消息")
  private String message;

  @Schema(description = "响应数据")
  private T data;

  public static <T> ApiResponse<T> success(T data) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setCode(200);
    response.setMessage("成功");
    response.setData(data);
    return response;
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setCode(200);
    response.setMessage(message);
    response.setData(data);
    return response;
  }

  public static <T> ApiResponse<T> error(String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setCode(500);
    response.setMessage(message);
    return response;
  }

  public static <T> ApiResponse<T> error(Integer code, String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setCode(code);
    response.setMessage(message);
    return response;
  }
}
