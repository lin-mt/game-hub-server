package com.app.gamehub.service;

import com.app.gamehub.dto.SendAllianceNotificationRequest;
import com.app.gamehub.dto.UpdateNotificationSettingsRequest;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.User;
import com.app.gamehub.enums.ActivityType;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.util.UserContext;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllianceNotificationService {

  private final AllianceRepository allianceRepository;
  private final GameAccountRepository gameAccountRepository;
  private final WeChatService weChatService;
  private final MessageSubscriptionService messageSubscriptionService;

  private static final String TEMPLATE_ID = "i4cXxSfmC7eSUaZxRL4u8sT_eBPCQ5RV_xmW0tmV85c";
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

  /** 发送联盟活动通知 */
  @Transactional
  public void sendAllianceNotification(SendAllianceNotificationRequest request) {
    Long currentUserId = UserContext.getUserId();

    // 验证联盟存在且当前用户是盟主
    Alliance alliance =
        allianceRepository
            .findById(request.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(currentUserId)) {
      throw new BusinessException("只有盟主可以发送联盟通知");
    }

    // 获取联盟所有成员账号
    List<GameAccount> members = gameAccountRepository.findByAllianceId(request.getAllianceId());

    String activityName = request.getActivityType().getDisplayName();
    String startTimeStr = request.getStartTime().format(DATE_FORMATTER);
    String sender = String.format("%s（%d区）", alliance.getName(), alliance.getServerId());

    int successCount = 0;
    int failCount = 0;

    for (GameAccount account : members) {
      if (account.getUserId() == null) {
        continue;
      }
      try {
        // 检查账号是否接收通知
        if (!shouldReceiveNotification(account, request.getActivityType())) {
          log.debug("账号 {} 未开启 {} 类型通知，跳过发送", account.getId(), activityName);
          continue;
        }

        // 检查用户是否有足够的订阅数量
        if (!messageSubscriptionService.hasEnoughSubscription(account.getUserId(), 1)) {
          log.warn("用户 {} 消息订阅数量不足，跳过发送通知", account.getUserId());
          continue;
        }

        // 获取用户当前剩余消息数量
        Integer remainingCount =
            messageSubscriptionService.getSubscriptionCount(account.getUserId());

        // 构建备注信息
        String remark = String.format("请及时参与活动，剩%d条通知", remainingCount - 1);

        // 发送微信通知
        sendWeChatNotification(
            account.getUser(),
            sender,
            activityName,
            startTimeStr,
            remark,
            request.getActivityType().page("id=%s".formatted(account.getId())));

        // 减少用户订阅数量
        messageSubscriptionService.decreaseSubscriptionCount(account.getUserId(), 1);

        successCount++;
        log.info("成功发送联盟通知给用户 {}, 活动: {}", account.getUserId(), activityName);

      } catch (Exception e) {
        failCount++;
        log.error(
            "发送联盟通知失败，用户: {}, 活动: {}, 错误: {}", account.getUserId(), activityName, e.getMessage());
      }
    }

    log.info("联盟通知发送完成，成功: {}, 失败: {}", successCount, failCount);
  }

  /** 更新账号通知设置 */
  @Transactional
  public GameAccount updateNotificationSettings(UpdateNotificationSettingsRequest request) {
    Long currentUserId = UserContext.getUserId();

    GameAccount account =
        gameAccountRepository
            .findById(request.getAccountId())
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    // 验证账号所有权
    if (!account.getUserId().equals(currentUserId)) {
      throw new BusinessException("只能修改自己的账号设置");
    }

    if (request.getNotificationTypes() != null) {
      account.setNotificationTypes(new HashSet<>(request.getNotificationTypes()));
    } else {
      account.setNotificationTypes(null);
    }

    GameAccount savedAccount = gameAccountRepository.save(account);
    log.info("更新账号 {} 通知设置成功", request.getAccountId());

    return savedAccount;
  }

  /** 检查账号是否应该接收指定类型的通知 */
  private boolean shouldReceiveNotification(GameAccount account, ActivityType activityType) {

    Set<ActivityType> notificationTypes = account.getNotificationTypes();
    if (notificationTypes == null || notificationTypes.isEmpty()) {
      return false;
    }

    return notificationTypes.contains(activityType);
  }

  /** 发送微信通知 */
  private void sendWeChatNotification(
      User user, String sender, String activityName, String startTime, String remark, String page) {
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
      weChatService.sendSubscribeMessage(user.getOpenid(), TEMPLATE_ID, templateData, page);

      log.info("微信通知发送成功 - 用户: {}, 发起方: {}, 活动: {}", user.getOpenid(), sender, activityName);

    } catch (Exception e) {
      log.error(
          "发送微信通知失败 - 用户: {}, 发起方: {}, 活动: {}, 错误: {}",
          user.getOpenid(),
          sender,
          activityName,
          e.getMessage());
      throw e;
    }
  }
}
