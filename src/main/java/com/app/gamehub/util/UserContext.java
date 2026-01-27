package com.app.gamehub.util;

/** 用户上下文工具类，基于ThreadLocal实现 */
public class UserContext {

  private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

  /** 获取当前用户ID */
  public static Long getUserId() {
    return USER_ID_HOLDER.get();
  }

  /** 设置当前用户ID */
  public static void setUserId(Long userId) {
    USER_ID_HOLDER.set(userId);
  }

  /** 清除当前用户ID */
  public static void clear() {
    USER_ID_HOLDER.remove();
  }
}
