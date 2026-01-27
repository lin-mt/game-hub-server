package com.app.gamehub.service;

import com.app.gamehub.dto.ReservePositionRequest;
import com.app.gamehub.dto.PositionReservationResultResponse;
import com.app.gamehub.dto.SetPositionReservationTimeRequest;
import com.app.gamehub.entity.Dynasty;
import com.app.gamehub.entity.DynastyPosition;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.PositionReservation;
import com.app.gamehub.entity.PositionType;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.DynastyPositionRepository;
import com.app.gamehub.repository.DynastyRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.repository.PositionReservationRepository;
import com.app.gamehub.util.UserContext;
import com.app.gamehub.util.Utils;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 官职预约服务类 */
@Slf4j
@Service
public class PositionReservationService {

  private final DynastyRepository dynastyRepository;
  private final DynastyPositionRepository dynastyPositionRepository;
  private final PositionReservationRepository positionReservationRepository;
  private final GameAccountRepository gameAccountRepository;

  // 用于管理预约锁的Map，key为 dynastyId + "_" + timeSlot
  private final Map<String, ReadWriteLock> reservationLocks = new ConcurrentHashMap<>();

  public PositionReservationService(
      DynastyRepository dynastyRepository,
      DynastyPositionRepository dynastyPositionRepository,
      PositionReservationRepository positionReservationRepository,
      GameAccountRepository gameAccountRepository) {
    this.dynastyRepository = dynastyRepository;
    this.dynastyPositionRepository = dynastyPositionRepository;
    this.positionReservationRepository = positionReservationRepository;
    this.gameAccountRepository = gameAccountRepository;
  }

  /** 获取预约锁，锁的key为王朝ID + "_" + 时段 */
  private ReadWriteLock getReservationLock(Long dynastyId, Integer timeSlot) {
    String lockKey = dynastyId + "_" + timeSlot;
    return reservationLocks.computeIfAbsent(lockKey, k -> new ReentrantReadWriteLock());
  }

  /** 设置官职预约时间 */
  @Transactional
  public DynastyPosition setPositionReservationTime(Long dynastyId, SetPositionReservationTimeRequest request) {
    // 验证用户是否为天子
    validateEmperor(dynastyId);

    // 验证时间设置的合理性
    validateReservationTimeSettings(request);

    // 查找或创建官职配置
    DynastyPosition position =
        dynastyPositionRepository
            .findByDynastyIdAndPositionType(dynastyId, request.getPositionType())
            .orElseThrow(() -> new BusinessException("官职配置不存在"));

    // 更新配置
    position.setReservationStartTime(request.getReservationStartTime());
    position.setReservationEndTime(request.getReservationEndTime());
    position.setDutyDate(request.getDutyDate());

    // 处理禁用时段
    String disabledSlots = "";
    if (request.getDisabledTimeSlots() != null && !request.getDisabledTimeSlots().isEmpty()) {
      disabledSlots =
          request.getDisabledTimeSlots().stream()
              .map(String::valueOf)
              .collect(Collectors.joining(","));
    }
    position.setDisabledTimeSlots(disabledSlots);

    DynastyPosition savedPosition = dynastyPositionRepository.save(position);

    // 清空该官职的历史预约结果
    positionReservationRepository.deleteByDutyDateBefore(request.getDutyDate().minusDays(2));

    log.info("王朝 {} 的 {} 官职预约时间已设置", dynastyId, request.getPositionType());
    return savedPosition;
  }

  /** 验证预约时间设置的合理性 */
  private void validateReservationTimeSettings(SetPositionReservationTimeRequest request) {
    LocalDateTime startTime = request.getReservationStartTime();
    LocalDateTime endTime = request.getReservationEndTime();
    LocalDate dutyDate = request.getDutyDate();

    // 验证开始时间早于结束时间
    if (!startTime.isBefore(endTime)) {
      throw new BusinessException("预约开始时间必须早于结束时间");
    }

    // 验证开始时间和结束时间在同一天
    if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
      throw new BusinessException("预约开始时间和结束时间必须在同一天");
    }

    // 验证任职日期在预约时间之后
    if (!dutyDate.isAfter(startTime.toLocalDate())) {
      throw new BusinessException("任职日期必须在预约时间之后");
    }

    // 验证禁用时段的有效性
    if (request.getDisabledTimeSlots() != null) {
      for (Integer slot : request.getDisabledTimeSlots()) {
        if (slot < 0 || slot > 23) {
          throw new BusinessException("时段必须在0-23之间");
        }
      }
    }
  }

  /** 预约官职 */
  @Transactional
  public PositionReservation reservePosition(Long dynastyId, Long accountId, ReservePositionRequest request) {
    Long userId = UserContext.getUserId();

    // 验证账号是否存在且属于当前用户
    GameAccount account =
        gameAccountRepository.findById(accountId).orElseThrow(() -> new BusinessException("账号不存在"));

    if (!account.getUserId().equals(userId)) {
      throw new BusinessException("无权操作此账号");
    }

    // 验证账号是否属于该王朝
    if (!dynastyId.equals(account.getDynastyId())) {
      throw new BusinessException("账号不属于该王朝");
    }

    // 验证王朝是否开启预约
    Dynasty dynasty =
        dynastyRepository.findById(dynastyId).orElseThrow(() -> new BusinessException("王朝不存在"));

    if (!dynasty.getReservationEnabled()) {
      throw new BusinessException("王朝未开启官职预约");
    }

    // 验证预约时间和条件
    validateReservationConditions(dynastyId, request);

    // 获取官职配置
    DynastyPosition position =
        dynastyPositionRepository
            .findByDynastyIdAndPositionType(dynastyId, request.getPositionType())
            .orElseThrow(() -> new BusinessException("官职配置不存在"));

    // 获取预约锁
    ReadWriteLock rwLock = getReservationLock(dynastyId, request.getTimeSlot());

    // 先使用读锁进行验证操作
    rwLock.readLock().lock();
    try {
      // 检查用户在该官职类型下是否已经预约了时段
      boolean hasReserved =
          positionReservationRepository.existsByAccountIdAndDynastyIdAndPositionTypeAndDutyDate(
              accountId, dynastyId, request.getPositionType(), position.getDutyDate());

      if (hasReserved) {
        throw new BusinessException("当天已有任职时段！");
      }

      // 检查该时段是否已被预约
      boolean isSlotTaken =
          positionReservationRepository.existsByDynastyIdAndPositionTypeAndDutyDateAndTimeSlot(
              dynastyId, request.getPositionType(), position.getDutyDate(), request.getTimeSlot());

      if (isSlotTaken) {
        throw new BusinessException("该时段的官职已被预约");
      }
    } finally {
      rwLock.readLock().unlock();
    }

    // 使用写锁进行数据写入操作
    rwLock.writeLock().lock();
    try {
      // 再次检查该时段是否已被预约（防止在读锁释放后被其他线程预约）
      boolean isSlotTaken =
          positionReservationRepository.existsByDynastyIdAndPositionTypeAndDutyDateAndTimeSlot(
              dynastyId, request.getPositionType(), position.getDutyDate(), request.getTimeSlot());

      if (isSlotTaken) {
        throw new BusinessException("该时段的官职已被预约");
      }

      // 创建预约记录
      PositionReservation reservation = new PositionReservation();
      reservation.setDynastyId(dynastyId);
      reservation.setPositionType(request.getPositionType());
      reservation.setDutyDate(position.getDutyDate());
      reservation.setTimeSlot(request.getTimeSlot());
      reservation.setAccountId(accountId);

      PositionReservation savedReservation = positionReservationRepository.save(reservation);

      log.info(
          "账号 {} 成功预约了王朝 {} 的 {} 官职时段 {}",
          account.getAccountName(),
          dynastyId,
          request.getPositionType(),
          request.getTimeSlot());

      return savedReservation;

    } finally {
      rwLock.writeLock().unlock();
    }
  }

  /** 验证预约条件 */
  private void validateReservationConditions(Long dynastyId, ReservePositionRequest request) {
    DynastyPosition position =
        dynastyPositionRepository
            .findByDynastyIdAndPositionType(dynastyId, request.getPositionType())
            .orElseThrow(() -> new BusinessException("官职配置不存在"));

    LocalDateTime now = LocalDateTime.now();

    // 验证预约时间是否已设置
    if (position.getReservationStartTime() == null || position.getReservationEndTime() == null) {
      throw new BusinessException("官职预约时间未设置");
    }

    // 验证当前时间是否在预约时间范围内
    if (now.isBefore(position.getReservationStartTime()) || now.isAfter(position.getReservationEndTime())) {
      throw new BusinessException(
          "官职预约的时间为：%s～%s"
              .formatted(
                  Utils.format(position.getReservationStartTime()),
                  Utils.format(position.getReservationEndTime())));
    }

    // 验证时段是否被禁用
    if (isTimeSlotDisabled(position, request.getTimeSlot())) {
      throw new BusinessException("该时段已被禁用，无法预约");
    }
  }

  /** 检查时段是否被禁用 */
  private boolean isTimeSlotDisabled(DynastyPosition position, Integer timeSlot) {
    if (position.getDisabledTimeSlots() == null || position.getDisabledTimeSlots().isEmpty()) {
      return false;
    }

    String[] disabledSlots = position.getDisabledTimeSlots().split(",");
    return Arrays.asList(disabledSlots).contains(timeSlot.toString());
  }

  /** 获取官职预约结果 */
  public PositionReservationResultResponse getReservationResults(Long dynastyId, LocalDate dutyDate) {
    // 验证王朝是否存在
    dynastyRepository.findById(dynastyId).orElseThrow(() -> new BusinessException("王朝不存在"));

    // 获取预约结果
    List<PositionReservation> allReservations =
        positionReservationRepository.findByDynastyIdAndDutyDateOrderByPositionTypeAscTimeSlotAsc(
            dynastyId, dutyDate);

    Map<PositionType, List<PositionReservation>> reservationsByType =
        allReservations.stream().collect(Collectors.groupingBy(PositionReservation::getPositionType));

    // 获取可用时段信息
    Map<PositionType, List<Integer>> availableSlots = new HashMap<>();
    for (PositionType positionType : PositionType.values()) {
      List<Integer> available = getAvailableTimeSlots(dynastyId, positionType, dutyDate);
      availableSlots.put(positionType, available);
    }

    PositionReservationResultResponse response = new PositionReservationResultResponse();
    response.setDynastyId(dynastyId);
    response.setDutyDate(dutyDate);
    response.setReservationResults(reservationsByType);
    response.setAvailableTimeSlots(availableSlots);

    return response;
  }

  /** 获取可用时段列表 */
  private List<Integer> getAvailableTimeSlots(
      Long dynastyId, PositionType positionType, LocalDate dutyDate) {
    // 获取官职配置
    DynastyPosition position =
        dynastyPositionRepository
            .findByDynastyIdAndPositionType(dynastyId, positionType)
            .orElse(null);

    if (position == null) {
      return Collections.emptyList();
    }

    // 获取已被预约的时段
    List<PositionReservation> reservations =
        positionReservationRepository.findByDynastyIdAndPositionTypeAndDutyDateOrderByTimeSlotAsc(
            dynastyId, positionType, dutyDate);
    Set<Integer> reservedSlots =
        reservations.stream().map(PositionReservation::getTimeSlot).collect(Collectors.toSet());

    // 获取被禁用的时段
    final Set<Integer> disabledSlots;
    if (position.getDisabledTimeSlots() != null && !position.getDisabledTimeSlots().isEmpty()) {
      String[] slots = position.getDisabledTimeSlots().split(",");
      disabledSlots = Arrays.stream(slots).map(Integer::parseInt).collect(Collectors.toSet());
    } else {
      disabledSlots = new HashSet<>();
    }

    // 返回可用时段（0-23中排除已预约和被禁用的）
    return IntStream.range(0, 24)
        .filter(slot -> !reservedSlots.contains(slot) && !disabledSlots.contains(slot))
        .boxed()
        .collect(Collectors.toList());
  }

  /** 验证用户是否为王朝天子 */
  private void validateEmperor(Long dynastyId) {
    Long userId = UserContext.getUserId();
    Dynasty dynasty =
        dynastyRepository.findById(dynastyId).orElseThrow(() -> new BusinessException("王朝不存在"));

    if (!dynasty.getEmperorId().equals(userId)) {
      throw new BusinessException("只有天子才能执行此操作");
    }
  }

  public DynastyPosition getPositionReservationTime(Long dynastyId, PositionType positionType) {
    return dynastyPositionRepository
        .findByDynastyIdAndPositionType(dynastyId, positionType)
        .orElse(null);
  }

  /**
   * 移除指定王朝的所有预约锁
   *
   * @param dynastyIds 王朝ID集合
   * @return 实际移除的锁数量
   */
  public int removeLock(Set<Long> dynastyIds) {
    if (dynastyIds == null || dynastyIds.isEmpty()) {
      return 0;
    }

    int removedCount = 0;
    try {
      // 使用迭代器安全地移除锁
      var iterator = reservationLocks.entrySet().iterator();
      while (iterator.hasNext()) {
        var entry = iterator.next();
        String lockKey = entry.getKey();

        // 安全地解析dynastyId
        String[] parts = lockKey.split("_");
        if (parts.length >= 1) {
          try {
            Long dynastyId = Long.parseLong(parts[0]);
            if (dynastyIds.contains(dynastyId)) {
              iterator.remove();
              removedCount++;
            }
          } catch (NumberFormatException e) {
            log.warn("无法解析锁键中的王朝ID: {}", lockKey, e);
          }
        }
      }
    } catch (Exception e) {
      log.error("移除预约锁时发生异常", e);
    }
    return removedCount;
  }

  public PositionReservation renouncePosition(
      Long dynastyId, Long accountId, @Valid ReservePositionRequest request) {
    // 获取官职配置
    Long userId = UserContext.getUserId();
    GameAccount account =
        gameAccountRepository.findById(accountId).orElseThrow(() -> new BusinessException("账号不存在"));
    Dynasty dynasty =
        dynastyRepository.findById(dynastyId).orElseThrow(() -> new BusinessException("王朝不存在"));
    if (!dynasty.getEmperorId().equals(userId) && !account.getUserId().equals(userId)) {
      throw new BusinessException("只有账号本人和王朝天子才可以取消官职");
    }

    DynastyPosition position =
        dynastyPositionRepository
            .findByDynastyIdAndPositionType(dynastyId, request.getPositionType())
            .orElseThrow(() -> new BusinessException("官职配置不存在"));

    PositionReservation positionReservation =
        positionReservationRepository.findByAccountIdAndDynastyIdAndPositionTypeAndDutyDate(
            accountId, dynastyId, request.getPositionType(), position.getDutyDate());
    if (positionReservation == null) {
      throw new BusinessException("当前时间段并无官职");
    }
    positionReservationRepository.deleteById(positionReservation.getId());
    return positionReservation;
  }
}