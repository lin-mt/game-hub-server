package com.app.gamehub.interceptor;

import com.app.gamehub.util.JwtUtil;
import com.app.gamehub.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

  private final JwtUtil jwtUtil;

  public JwtInterceptor(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
      throws Exception {
    // 跳过OPTIONS请求
    if ("OPTIONS".equals(request.getMethod())) {
      return true;
    }

    String token = getTokenFromRequest(request);

    if (token == null || !jwtUtil.validateToken(token) || jwtUtil.isTokenExpired(token)) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\":401,\"message\":\"未授权访问\"}");
      return false;
    }

    // 将用户ID存储到ThreadLocal中
    Long userId = jwtUtil.getUserIdFromToken(token);
    UserContext.setUserId(userId);

    return true;
  }

  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      Exception ex)
      throws Exception {
    // 清除ThreadLocal中的用户ID
    UserContext.clear();
  }

  private String getTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
