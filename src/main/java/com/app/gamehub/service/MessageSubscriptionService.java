package com.app.gamehub.service;

import com.app.gamehub.entity.User;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSubscriptionService {

  private final UserRepository userRepository;

  /**
   * 增加用户消息订阅数量
   *
   * @param userId 用户ID
   * @param count 增加的数量
   * @return 更新后的用户信息
   */
  @Transactional
  public User addSubscriptionCount(Long userId, Integer count) {
    User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));

    int newCount = user.getMessageSubscriptionCount() + count;
    user.setMessageSubscriptionCount(newCount);

    User savedUser = userRepository.save(user);
    log.info("用户 {} 增加消息订阅数量 {}，当前总数: {}", userId, count, newCount);

    return savedUser;
  }

  /**
   * 减少用户消息订阅数量（发送消息时调用）
   *
   * @param userId 用户ID
   * @param count 减少的数量
   * @return 更新后的用户信息
   */
  @Transactional
  public User decreaseSubscriptionCount(Long userId, Integer count) {
    User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));

    int currentCount = user.getMessageSubscriptionCount();
    if (currentCount < count) {
      throw new BusinessException("用户可接收消息数量不足");
    }

    int newCount = currentCount - count;
    user.setMessageSubscriptionCount(newCount);

    User savedUser = userRepository.save(user);
    log.info("用户 {} 减少消息订阅数量 {}，当前剩余: {}", userId, count, newCount);

    return savedUser;
  }

  /**
   * 获取用户当前可接收消息数量
   *
   * @param userId 用户ID
   * @return 可接收消息数量
   */
  public Integer getSubscriptionCount(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));

    return user.getMessageSubscriptionCount();
  }

  /**
   * 检查用户是否有足够的消息订阅数量
   *
   * @param userId 用户ID
   * @param requiredCount 需要的数量
   * @return 是否有足够的数量
   */
  public boolean hasEnoughSubscription(Long userId, Integer requiredCount) {
    Integer currentCount = getSubscriptionCount(userId);
    return currentCount >= requiredCount;
  }

  public Boolean clearSubscriptionCount(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
    user.setMessageSubscriptionCount(0);
    userRepository.saveAndFlush(user);
    return true;
  }
}
