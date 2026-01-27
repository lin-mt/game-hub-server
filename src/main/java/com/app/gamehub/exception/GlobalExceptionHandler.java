package com.app.gamehub.exception;

import com.app.gamehub.dto.ApiResponse;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 全局异常处理器 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** 处理业务异常 */
  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleBusinessException(BusinessException e) {
    log.warn("业务异常: {}", e.getMessage());
    return ApiResponse.error(e.getCode(), e.getMessage());
  }

  /** 处理参数验证异常 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    log.warn("参数验证异常: {}", message);
    return ApiResponse.error(400, message);
  }

  /** 处理绑定异常 */
  @ExceptionHandler(BindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleBindException(BindException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    log.warn("参数绑定异常: {}", message);
    return ApiResponse.error(400, message);
  }

  /** 处理数字格式异常 */
  @ExceptionHandler(NumberFormatException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> handleNumberFormatException(NumberFormatException e) {
    log.warn("数字格式异常: {}", e.getMessage());
    return ApiResponse.error(400, "参数格式错误");
  }

  /** 处理静态资源未找到异常 */
  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiResponse<Void> handleNoResourceFoundException(NoResourceFoundException e) {
    log.warn("No static resource {}", e.getResourcePath());
    return ApiResponse.error(404, "资源未找到");
  }

  /** 处理其他所有异常 */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> handleException(Exception e) {
    log.error("系统异常", e);
    return ApiResponse.error(500, "系统出错，请联系相关人员");
  }
}
