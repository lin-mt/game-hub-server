package com.app.gamehub.controller;

import com.app.gamehub.dto.ApiResponse;
import com.app.gamehub.entity.User;
import com.app.gamehub.service.MessageSubscriptionService;
import com.app.gamehub.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/message-subscription")
@Tag(name = "消息订阅管理", description = "微信消息订阅相关接口")
public class MessageSubscriptionController {

  private final MessageSubscriptionService messageSubscriptionService;

  public MessageSubscriptionController(MessageSubscriptionService messageSubscriptionService) {
    this.messageSubscriptionService = messageSubscriptionService;
  }

  @PostMapping("/add")
  @Operation(summary = "增加消息订阅数量")
  public ApiResponse<User> addSubscriptionCount() {
    Long userId = UserContext.getUserId();
    User user = messageSubscriptionService.addSubscriptionCount(userId, 1);
    return ApiResponse.success("消息订阅数量增加成功", user);
  }

  @GetMapping("/count")
  @Operation(summary = "获取当前可接收消息数量")
  public ApiResponse<Integer> getSubscriptionCount() {
    Long userId = UserContext.getUserId();
    Integer count = messageSubscriptionService.getSubscriptionCount(userId);
    return ApiResponse.success(count);
  }

  @GetMapping("/check/{requiredCount}")
  @Operation(summary = "检查是否有足够的消息订阅数量")
  public ApiResponse<Boolean> checkSubscription(@PathVariable Integer requiredCount) {
    Long userId = UserContext.getUserId();
    Boolean hasEnough = messageSubscriptionService.hasEnoughSubscription(userId, requiredCount);
    return ApiResponse.success(hasEnough);
  }

    @PostMapping("/clear")
    @Operation(summary = "清空订阅数量")
    public ApiResponse<Boolean> clearSubscription() {
        Long userId = UserContext.getUserId();
        Boolean cleared = messageSubscriptionService.clearSubscriptionCount(userId);
        return ApiResponse.success(cleared);
    }
}
