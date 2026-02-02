package com.app.gamehub.exception;

/**
 * OCR属性数据错误异常 - 不可挽回的错误
 * 当属性数据格式错误，确认包含特定属性信息时抛出
 * 此异常应该直接抛出，不再尝试其他OCR服务商
 */
public class OcrAttributeException extends RuntimeException {

  public OcrAttributeException(String message) {
    super(message);
  }

  public OcrAttributeException(String message, Throwable cause) {
    super(message, cause);
  }
}
