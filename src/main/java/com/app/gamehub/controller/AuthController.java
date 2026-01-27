package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.dto.LoginRequest;
import com.app.gamehub.dto.LoginResponse;
import com.app.gamehub.dto.UserInfoResponse;
import com.app.gamehub.entity.User;
import com.app.gamehub.service.AuthService;
import com.app.gamehub.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  @Operation(summary = "微信小程序登录")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = authService.login(request);
    return ApiResponse.success(response);
  }

  @GetMapping("/user-info")
  @Operation(summary = "获取用户信息")
  public ApiResponse<User> getUserInfo() {
    Long userId = UserContext.getUserId();
    User user = authService.getUserInfo(userId);
    return ApiResponse.success(user);
  }

  @GetMapping("/user-complete-info")
  @Operation(summary = "获取用户完整信息（包含联盟和账号）")
  public ApiResponse<UserInfoResponse> getUserCompleteInfo() {
    Long userId = UserContext.getUserId();
    UserInfoResponse response = authService.getUserCompleteInfo(userId);
    return ApiResponse.success(response);
  }
}
