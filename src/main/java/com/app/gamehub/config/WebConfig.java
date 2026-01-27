package com.app.gamehub.config;

import com.app.gamehub.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final JwtInterceptor jwtInterceptor;

  public WebConfig(JwtInterceptor jwtInterceptor) {
    this.jwtInterceptor = jwtInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(jwtInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/auth/login", "/swagger-ui/**", "/api-docs/**", "/swagger-ui.html");
  }

}
