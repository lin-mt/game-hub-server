package com.app.gamehub.service;

import com.app.gamehub.dto.UpdateArenaNotificationRequest;
import com.app.gamehub.entity.User;
import com.app.gamehub.enums.ActivityType;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.UserRepository;
import com.app.gamehub.util.UserContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaNotificationService {

  private final UserRepository userRepository;
  private final WeChatService weChatService;
  private final MessageSubscriptionService messageSubscriptionService;

  private static final String TEMPLATE_ID = "i4cXxSfmC7eSUaZxRL4u8sT_eBPCQ5RV_xmW0tmV85c";
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

  /**
   * 更新用户演武场通知设置
   *
   * @param request 更新请求
   * @return 更新后的用户信息
   */
  @Transactional
  public User updateArenaNotificationSetting(UpdateArenaNotificationRequest request) {
    Long userId = UserContext.getUserId();
    User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));

    user.setArenaNotificationEnabled(request.getEnabled());
    User savedUser = userRepository.save(user);

    log.info("用户 {} 更新演武场通知设置为: {}", userId, request.getEnabled());
    return savedUser;
  }

  /**
   * 获取用户演武场通知设置
   *
   * @return 是否启用演武场通知
   */
  public Boolean getArenaNotificationSetting() {
    Long userId = UserContext.getUserId();
    User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
    return user.getArenaNotificationEnabled();
  }

  /**
   * 发送演武场通知给所有启用了通知的用户
   */
  @Transactional
  public void sendArenaNotifications() {
    log.info("开始发送演武场通知");

    // 查找所有启用了演武场通知的用户
    List<User> enabledUsers = userRepository.findByArenaNotificationEnabledTrue();

    if (enabledUsers.isEmpty()) {
      log.info("没有用户启用演武场通知，跳过发送");
      return;
    }

    String activityName = ActivityType.YAN_WU_CHANG.getDisplayName();
    String sender = "战策宝";
    LocalDateTime now = LocalDateTime.now();
    String startTimeStr = now.format(DATE_FORMATTER);

    int successCount = 0;
    int failCount = 0;

    for (User user : enabledUsers) {
      try {
        // 检查用户是否有足够的订阅数量
        if (!messageSubscriptionService.hasEnoughSubscription(user.getId(), 1)) {
          log.warn("用户 {} 消息订阅数量不足，跳过发送演武场通知", user.getId());
          continue;
        }

        // 获取用户当前剩余消息数量
        Integer remainingCount = messageSubscriptionService.getSubscriptionCount(user.getId());

        // 构建备注信息
        String remark = String.format("演武场即将结束，剩%d条通知", remainingCount - 1);

        // 发送微信通知
        sendWeChatNotification(user, sender, activityName, startTimeStr, remark);

        // 减少用户订阅数量
        messageSubscriptionService.decreaseSubscriptionCount(user.getId(), 1);

        successCount++;
        log.info("成功发送演武场通知给用户 {}", user.getId());

      } catch (Exception e) {
        failCount++;
        log.error("发送演武场通知失败，用户: {}, 错误: {}", user.getId(), e.getMessage());
      }
    }

    log.info("演武场通知发送完成，成功: {}, 失败: {}", successCount, failCount);
  }

  /**
   * 发送微信通知
   */
  private void sendWeChatNotification(
      User user, String sender, String activityName, String startTime, String remark) {
    try {
      // 构建模板数据
      Map<String, Object> templateData = new HashMap<>();

      // 发起方 {{thing1.DATA}}
      Map<String, Object> thing1 = new HashMap<>();
      thing1.put("value", sender);
      templateData.put("thing1", thing1);

      // 活动名称 {{thing6.DATA}}
      Map<String, Object> thing6 = new HashMap<>();
      thing6.put("value", activityName);
      templateData.put("thing6", thing6);

      // 开始时间 {{date2.DATA}}
      Map<String, Object> date2 = new HashMap<>();
      date2.put("value", startTime);
      templateData.put("date2", date2);

      // 备注 {{thing11.DATA}}
      Map<String, Object> thing11 = new HashMap<>();
      thing11.put("value", remark);
      templateData.put("thing11", thing11);

      // 发送微信订阅消息
      weChatService.sendSubscribeMessage(user.getOpenid(), TEMPLATE_ID, templateData, null);

    } catch (Exception e) {
      throw e;
    }
  }
}
