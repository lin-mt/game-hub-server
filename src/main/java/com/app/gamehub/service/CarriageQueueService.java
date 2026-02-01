package com.app.gamehub.service;

import com.app.gamehub.dto.CarriageQueueItemResponse;
import com.app.gamehub.dto.CarriageQueueListResponse;
import com.app.gamehub.dto.JoinCarriageQueueRequest;
import com.app.gamehub.dto.LeaveCarriageQueueRequest;
import com.app.gamehub.entity.CarriageQueue;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.CarriageQueueRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.util.UserContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarriageQueueService {

  private final CarriageQueueRepository carriageQueueRepository;
  private final GameAccountRepository gameAccountRepository;
  private final AllianceRepository allianceRepository;

  /**
   * 报名马车
   *
   * @param request 报名请求
   * @return 排队记录
   */
  @Transactional
  public CarriageQueue joinQueue(JoinCarriageQueueRequest request) {
    Long allianceId = request.getAllianceId();
    Long accountId = request.getAccountId();

    // 验证联盟是否存在
    if (!allianceRepository.existsById(allianceId)) {
      throw new BusinessException("联盟不存在");
    }

    // 验证游戏账号是否存在
    GameAccount gameAccount =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    // 验证账号是否属于当前用户
    Long userId = UserContext.getUserId();
    if (gameAccount.getUserId() == null || !gameAccount.getUserId().equals(userId)) {
      throw new BusinessException("只能为自己的游戏账号报名");
    }

    // 验证账号是否属于该联盟
    if (!allianceId.equals(gameAccount.getAllianceId())) {
      throw new BusinessException("该账号不属于此联盟");
    }

    // 检查账号是否已在排队列表中
    if (carriageQueueRepository.existsByAllianceIdAndAccountId(allianceId, accountId)) {
      throw new BusinessException("该账号已在排队列表中，无法重复报名");
    }

    // 获取当前最大排队顺序号
    Integer maxOrder = carriageQueueRepository.findMaxQueueOrderByAllianceId(allianceId);

    // 创建排队记录
    CarriageQueue carriageQueue = new CarriageQueue();
    carriageQueue.setAllianceId(allianceId);
    carriageQueue.setAccountId(accountId);
    carriageQueue.setQueueOrder(maxOrder + 1);

    CarriageQueue savedQueue = carriageQueueRepository.save(carriageQueue);
    log.info("账号 {} 成功报名联盟 {} 的马车，排队顺序: {}", accountId, allianceId, savedQueue.getQueueOrder());

    return savedQueue;
  }

  /**
   * 退出马车队列
   *
   * @param request 退出请求
   */
  @Transactional
  public void leaveQueue(LeaveCarriageQueueRequest request) {
    Long allianceId = request.getAllianceId();
    Long accountId = request.getAccountId();

    // 验证游戏账号是否存在
    GameAccount gameAccount =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    // 验证账号是否属于当前用户
    Long userId = UserContext.getUserId();
    if (gameAccount.getUserId() == null || !gameAccount.getUserId().equals(userId)) {
      throw new BusinessException("只能退出自己的游戏账号");
    }

    // 查找排队记录
    CarriageQueue carriageQueue =
        carriageQueueRepository
            .findByAllianceIdAndAccountId(allianceId, accountId)
            .orElseThrow(() -> new BusinessException("该账号不在排队列表中"));

    Integer removedOrder = carriageQueue.getQueueOrder();

    // 删除排队记录
    carriageQueueRepository.delete(carriageQueue);

    // 更新后续排队顺序
    carriageQueueRepository.decrementQueueOrderAfter(allianceId, removedOrder);

    log.info("账号 {} 已退出联盟 {} 的马车队列", accountId, allianceId);
  }

  /**
   * 查询联盟马车排队列表
   *
   * @param allianceId 联盟ID
   * @return 排队列表响应
   */
  public CarriageQueueListResponse getQueueList(Long allianceId) {
    // 验证联盟是否存在
    if (!allianceRepository.existsById(allianceId)) {
      throw new BusinessException("联盟不存在");
    }

    // 查询排队列表
    List<CarriageQueue> queueList =
        carriageQueueRepository.findByAllianceIdOrderByQueueOrderAsc(allianceId);

    Long todayDriverAccountId = null;
    String todayDriverAccountName = null;

    // 构建响应列表
    List<CarriageQueueItemResponse> responseList = new ArrayList<>();

    if (!CollectionUtils.isEmpty(queueList)) {
      Map<Long, String> id2name =
          gameAccountRepository
              .findAllById(queueList.stream().map(CarriageQueue::getAccountId).toList())
              .stream()
              .collect(Collectors.toMap(GameAccount::getId, GameAccount::getAccountName));
      LocalDate today = LocalDate.now();
      for (CarriageQueue queue : queueList) {
        String accountName = id2name.getOrDefault(queue.getAccountId(), "未知账号");
        if (today.equals(queue.getLastDriveDate())) {
          todayDriverAccountId = queue.getAccountId();
          todayDriverAccountName = accountName;
        }
        CarriageQueueItemResponse item =
            CarriageQueueItemResponse.builder()
                .id(queue.getId())
                .accountId(queue.getAccountId())
                .accountName(accountName)
                .queueOrder(queue.getQueueOrder())
                .isTodayDriver(queue.getAccountId().equals(todayDriverAccountId))
                .lastDriveDate(queue.getLastDriveDate())
                .createdAt(queue.getCreatedAt())
                .build();

        responseList.add(item);
      }
    }

    return CarriageQueueListResponse.builder()
        .allianceId(allianceId)
        .totalCount(queueList.size())
        .todayDriverAccountId(todayDriverAccountId)
        .todayDriverAccountName(todayDriverAccountName)
        .queueList(responseList)
        .build();
  }

  /** 检查并标记今日马车车主 此方法由定时任务每30分钟调用 */
  @Transactional
  public void checkAndAssignTodayDriver() {
    log.info("开始检查并分配今日马车车主");
    LocalDate today = LocalDate.now();

    // 获取所有有排队记录的联盟ID
    List<Long> allianceIds = carriageQueueRepository.findDistinctAllianceIds();

    int removedCount = 0;
    int assignedCount = 0;
    for (Long allianceId : allianceIds) {
      // 查找并删除前一天的车主（lastDriveDate不是今天且不为null的）
      List<CarriageQueue> previousDrivers =
          carriageQueueRepository.findByAllianceIdAndLastDriveDateNotNullAndLastDriveDateNot(
              allianceId, today);

      for (CarriageQueue previousDriver : previousDrivers) {
        Integer removedOrder = previousDriver.getQueueOrder();
        carriageQueueRepository.delete(previousDriver);
        // 更新后续排队顺序
        carriageQueueRepository.decrementQueueOrderAfter(allianceId, removedOrder);
        removedCount++;
        log.info("联盟 {} 的前一天车主账号 {} 已移出排队列表", allianceId, previousDriver.getAccountId());
      }

      // 检查该联盟今日是否已有车主
      Optional<CarriageQueue> todayDriverOpt =
          carriageQueueRepository.findFirstByAllianceIdAndLastDriveDate(allianceId, today);

      if (todayDriverOpt.isEmpty()) {
        // 没有今日车主，将queueOrder最小的账号标记为今日车主
        Optional<CarriageQueue> firstInQueueOpt =
            carriageQueueRepository.findFirstByAllianceIdOrderByQueueOrderAsc(allianceId);

        if (firstInQueueOpt.isPresent()) {
          CarriageQueue firstInQueue = firstInQueueOpt.get();

          // 标记为今日车主
          firstInQueue.setLastDriveDate(today);
          carriageQueueRepository.save(firstInQueue);

          assignedCount++;
          log.info("联盟 {} 的今日马车车主已分配给账号 {}", allianceId, firstInQueue.getAccountId());
        }
      }
    }

    log.info("马车车主检查任务完成，共移除了 {} 个前一天车主，分配了 {} 个今日车主", removedCount, assignedCount);
  }

  /** 根据联盟ID删除所有排队记录（用于删除联盟时） */
  @Transactional
  public void deleteByAllianceId(Long allianceId) {
    carriageQueueRepository.deleteByAllianceId(allianceId);
    log.info("已删除联盟 {} 的所有马车排队记录", allianceId);
  }

  /**
   * 设置今日车主 如果今日已有车主，将原车主插入到队伍第一名 如果账号不在排队列表中，会自动加入到队伍第一位
   *
   * @param allianceId 联盟ID
   * @param accountId 账号ID
   */
  @Transactional
  public void setTodayDriver(Long allianceId, Long accountId) {
    // 验证联盟是否存在
    if (!allianceRepository.existsById(allianceId)) {
      throw new BusinessException("联盟不存在");
    }

    // 验证游戏账号是否存在
    GameAccount gameAccount =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    // 验证账号是否属于该联盟
    if (!allianceId.equals(gameAccount.getAllianceId())) {
      throw new BusinessException("该账号不属于此联盟");
    }

    LocalDate today = LocalDate.now();

    // 查找今日是否已有车主
    Optional<CarriageQueue> currentDriverOpt =
        carriageQueueRepository.findFirstByAllianceIdAndLastDriveDate(allianceId, today);

    // 查找该账号的排队记录
    Optional<CarriageQueue> targetQueueOpt =
        carriageQueueRepository.findByAllianceIdAndAccountId(allianceId, accountId);

    CarriageQueue targetQueue;

    if (targetQueueOpt.isEmpty()) {
      // 账号不在排队列表中，将所有现有成员的排队顺序+1，然后插入到第一位
      carriageQueueRepository.incrementQueueOrderFrom(allianceId, 1);

      targetQueue = new CarriageQueue();
      targetQueue.setAllianceId(allianceId);
      targetQueue.setAccountId(accountId);
      targetQueue.setQueueOrder(1); // 插入到第一位
      targetQueue = carriageQueueRepository.save(targetQueue);
      log.info("账号 {} 自动加入联盟 {} 的马车队伍第一位", accountId, allianceId);
    } else {
      targetQueue = targetQueueOpt.get();

      // 如果当前车主就是要设置的账号，无需操作
      if (currentDriverOpt.isPresent() && currentDriverOpt.get().getAccountId().equals(accountId)) {
        log.info("账号 {} 已经是联盟 {} 的今日车主", accountId, allianceId);
        return;
      }
    }

    if (currentDriverOpt.isPresent()) {
      CarriageQueue currentDriver = currentDriverOpt.get();

      // 将原车主的lastDriveDate清空
      currentDriver.setLastDriveDate(null);

      // 1. 所有排队顺序>=0的记录+1
      carriageQueueRepository.incrementQueueOrderFrom(allianceId, 0);

      targetQueue =
          carriageQueueRepository
              .findByAllianceIdAndAccountId(allianceId, accountId)
              .orElseThrow(() -> new BusinessException("该账号不在排队列表中"));
    }

    // 设置新的今日车主
    targetQueue.setLastDriveDate(today);
    carriageQueueRepository.save(targetQueue);

    log.info("联盟 {} 的今日车主已设置为账号 {}", allianceId, accountId);
  }

  /**
   * 从马车队伍中移除成员
   *
   * @param allianceId 联盟ID
   * @param accountId 账号ID
   */
  @Transactional
  public void removeMember(Long allianceId, Long accountId) {
    // 验证联盟是否存在
    if (!allianceRepository.existsById(allianceId)) {
      throw new BusinessException("联盟不存在");
    }

    // 查找排队记录
    CarriageQueue carriageQueue =
        carriageQueueRepository
            .findByAllianceIdAndAccountId(allianceId, accountId)
            .orElseThrow(() -> new BusinessException("该账号不在排队列表中"));

    Integer removedOrder = carriageQueue.getQueueOrder();

    // 删除排队记录
    carriageQueueRepository.delete(carriageQueue);

    // 更新后续排队顺序
    carriageQueueRepository.decrementQueueOrderAfter(allianceId, removedOrder);

    log.info("账号 {} 已从联盟 {} 的马车队伍中移除", accountId, allianceId);
  }

  /**
   * 在马车队伍中插队（添加成员）
   *
   * @param allianceId 联盟ID
   * @param accountId 账号ID
   * @param position 插入位置（从1开始）
   */
  @Transactional
  public void insertMember(Long allianceId, Long accountId, Integer position) {
    // 验证联盟是否存在
    if (!allianceRepository.existsById(allianceId)) {
      throw new BusinessException("联盟不存在");
    }

    // 验证游戏账号是否存在
    GameAccount gameAccount =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    // 验证账号是否属于该联盟
    if (!allianceId.equals(gameAccount.getAllianceId())) {
      throw new BusinessException("该账号不属于此联盟");
    }

    // 检查账号是否已在排队列表中
    if (carriageQueueRepository.existsByAllianceIdAndAccountId(allianceId, accountId)) {
      throw new BusinessException("该账号已在排队列表中，无法重复添加");
    }

    // 验证插入位置
    if (position < 1) {
      throw new BusinessException("插入位置必须大于等于1");
    }

    // 获取当前最大排队顺序号
    Integer maxOrder = carriageQueueRepository.findMaxQueueOrderByAllianceId(allianceId);

    // 如果插入位置超过最大值，则插入到末尾
    if (position > maxOrder + 1) {
      position = maxOrder + 1;
    }

    // 将插入位置及之后的记录排队顺序+1
    carriageQueueRepository.incrementQueueOrderFrom(allianceId, position);

    // 创建排队记录
    CarriageQueue carriageQueue = new CarriageQueue();
    carriageQueue.setAllianceId(allianceId);
    carriageQueue.setAccountId(accountId);
    carriageQueue.setQueueOrder(position);

    carriageQueueRepository.save(carriageQueue);
    log.info("账号 {} 已插入到联盟 {} 的马车队伍第 {} 位", accountId, allianceId, position);
  }
}
