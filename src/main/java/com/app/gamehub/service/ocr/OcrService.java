package com.app.gamehub.service.ocr;

import java.io.File;
import java.util.Map;

/** OCR服务抽象接口 支持多个OCR服务商的统一接口 */
public interface OcrService {

  /**
   * 识别图片中的属性信息
   *
   * @param imageFile 图片文件
   * @return 属性Map，key为属性名，value为属性值
   * @throws Exception 识别失败时抛出异常
   */
  Map<String, Integer> parseStats(File imageFile) throws Exception;

  /**
   * 获取服务商名称
   *
   * @return 服务商名称
   */
  String getProviderName();

  /**
   * 检查服务是否可用
   *
   * @return true表示可用，false表示不可用
   */
  boolean isAvailable();
}
