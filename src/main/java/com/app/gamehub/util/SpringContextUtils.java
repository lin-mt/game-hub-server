package com.app.gamehub.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContextUtils implements ApplicationContextAware {

  private static ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    context = applicationContext;
  }

  public static <T> T getBean(Class<T> clazz) {
    if (context == null) return null;
    return context.getBean(clazz);
  }

  public static Object getBean(String name) {
    if (context == null) return null;
    return context.getBean(name);
  }
}

