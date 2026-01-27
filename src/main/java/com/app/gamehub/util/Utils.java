package com.app.gamehub.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
  // 统一的格式化器
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

  public static String format(LocalDateTime dateTime) {
    return dateTime.format(FORMATTER);
  }

}
