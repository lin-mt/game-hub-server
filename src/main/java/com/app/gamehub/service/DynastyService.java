package com.app.gamehub.service;

import com.app.gamehub.dto.*;
import com.app.gamehub.entity.*;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.*;
import com.app.gamehub.util.AllianceCodeGenerator;
import com.app.gamehub.util.UserContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 王朝服务类 */
@Slf4j
@Service
public class DynastyService {

  private final DynastyRepository dynastyRepository;
  private final DynastyPositionRepository dynastyPositionRepository;
  private final PositionReservationRepository positionReservationRepository;
  private final GameAccountRepository gameAccountRepository;
  private final UserRepository userRepository;
  private final AllianceCodeGenerator codeGenerator;

  public DynastyService(
      DynastyRepository dynastyRepository,
      DynastyPositionRepository dynastyPositionRepository,
      PositionReservationRepository positionReservationRepository,
      GameAccountRepository gameAccountRepository,
      UserRepository userRepository,
      AllianceCodeGenerator codeGenerator) {
    this.dynastyRepository = dynastyRepository;
    this.dynastyPositionRepository = dynastyPositionRepository;
    this.positionReservationRepository = positionReservationRepository;
    this.gameAccountRepository = gameAccountRepository;
    this.userRepository = userRepository;
    this.codeGenerator = codeGenerator;
  }

  /** 创建王朝 */
  @Transactional
  public Dynasty createDynasty(CreateDynastyRequest request) {
    Long userId = UserContext.getUserId();

    // 验证用户是否存在
    if (!userRepository.existsById(userId)) {
      throw new BusinessException("用户不存在");
    }

    // 检查用户在该区是否已创建王朝
    if (dynastyRepository.existsByEmperorIdAndServerId(userId, request.getServerId())) {
      throw new BusinessException("您在该区已创建王朝，每个用户在每个区只能创建一个王朝");
    }

    // 生成唯一的王朝编码
    String code;
    do {
      code = codeGenerator.generateCode();
    } while (dynastyRepository.existsByCode(code));

    Dynasty dynasty = new Dynasty();
    dynasty.setName(request.getName());
    dynasty.setCode(code);
    dynasty.setServerId(request.getServerId());
    dynasty.setEmperorId(userId);
    dynasty.setReservationEnabled(true);

    Dynasty savedDynasty = dynastyRepository.save(dynasty);

    // 初始化官职配置
    initializePositions(savedDynasty.getId());

    log.info("用户 {} 在区 {} 创建了王朝: {}", userId, request.getServerId(), savedDynasty.getName());
    return savedDynasty;
  }

  /** 初始化官职配置 */
  private void initializePositions(Long dynastyId) {
    for (PositionType positionType : PositionType.values()) {
      DynastyPosition position = new DynastyPosition();
      position.setDynastyId(dynastyId);
      position.setPositionType(positionType);
      dynastyPositionRepository.save(position);
    }
  }

  /** 根据编码获取王朝信息 */
  public Dynasty getDynastyByCode(String code) {
    return dynastyRepository.findByCode(code).orElseThrow(() -> new BusinessException("王朝不存在"));
  }

  /** 获取王朝详情 */
  public DynastyDetailResponse getDynastyDetail(Long dynastyId) {
    Dynasty dynasty =
        dynastyRepository.findById(dynastyId).orElseThrow(() -> new BusinessException("王朝不存在"));

    List<DynastyPosition> positions = dynastyPositionRepository.findByDynastyId(dynastyId);

    // 获取最新的预约结果（当前日期或最近的任职日期）
    LocalDate today = LocalDate.now();
    List<PositionReservation> allReservations =
        positionReservationRepository.findByDynastyIdAndDutyDateOrderByPositionTypeAscTimeSlotAsc(
            dynastyId, today);
    Map<String, List<PositionReservation>> reservationsByType =
        allReservations.stream().collect(Collectors.groupingBy(reservation -> reservation.getPositionType().name()));

    DynastyDetailResponse response = new DynastyDetailResponse();
    response.setDynasty(dynasty);
    response.setPositions(positions);
    response.setPositionReservations(reservationsByType);

    return response;
  }

  /** 获取用户创建的王朝列表 */
  public List<Dynasty> getUserDynasties() {
    Long userId = UserContext.getUserId();
    return dynastyRepository.findByEmperorIdOrderByServerIdDesc(userId);
  }

  /** 账号加入王朝 */
  @Transactional
  public GameAccount joinDynasty(Long accountId, JoinDynastyRequest request) {
    Long userId = UserContext.getUserId();

    // 验证账号是否存在且属于当前用户
    GameAccount account =
        gameAccountRepository.findById(accountId).orElseThrow(() -> new BusinessException("账号不存在"));

    if (!account.getUserId().equals(userId)) {
      throw new BusinessException("无权操作此账号");
    }

    // 验证王朝是否存在
    Dynasty dynasty = getDynastyByCode(request.getDynastyCode());

    // 验证账号是否已加入王朝
    if (account.getDynastyId() != null && account.getDynastyId().equals(dynasty.getId())) {
      return account;
    }

    // 验证账号和王朝是否在同一个区
    if (!account.getServerId().equals(dynasty.getServerId())) {
      throw new BusinessException("账号只能加入同一个区的王朝");
    }

    // 加入王朝
    account.setDynastyId(dynasty.getId());
    GameAccount savedAccount = gameAccountRepository.save(account);

    log.info("账号 {} 加入了王朝 {}", account.getAccountName(), dynasty.getName());
    return savedAccount;
  }

  /** 验证用户是否为王朝天子 */
  public void validateEmperor(Long dynastyId) {
    Long userId = UserContext.getUserId();
    Dynasty dynasty =
        dynastyRepository.findById(dynastyId).orElseThrow(() -> new BusinessException("王朝不存在"));

    if (!dynasty.getEmperorId().equals(userId)) {
      throw new BusinessException("只有天子才能执行此操作");
    }
  }

  /** 开启或关闭官职预约 */
  @Transactional
  public Dynasty toggleReservationEnabled(Long dynastyId, boolean enabled) {
    validateEmperor(dynastyId);

    Dynasty dynasty =
        dynastyRepository.findById(dynastyId).orElseThrow(() -> new BusinessException("王朝不存在"));

    dynasty.setReservationEnabled(enabled);
    Dynasty savedDynasty = dynastyRepository.save(dynasty);

    log.info("王朝 {} 的官职预约状态已{}为: {}", dynasty.getName(), enabled ? "开启" : "关闭", enabled);
    return savedDynasty;
  }

  /** 开启或关闭自动配置官职预约 */
  @Transactional
  public Dynasty toggleAutoConfigureReservation(Long dynastyId, boolean enabled) {
    validateEmperor(dynastyId);

    Dynasty dynasty =
        dynastyRepository.findById(dynastyId).orElseThrow(() -> new BusinessException("王朝不存在"));

    dynasty.setAutoConfigureReservation(enabled);
    Dynasty savedDynasty = dynastyRepository.save(dynasty);

    log.info("王朝 {} 的自动配置官职预约状态已更新为: {}", dynastyId, enabled);
    return savedDynasty;
  }

  /** 清空王朝所有预约结果 */
  @Transactional
  public void clearAllReservationResults(Long dynastyId) {
    validateEmperor(dynastyId);

    positionReservationRepository.deleteByDynastyId(dynastyId);
    log.info("已清空王朝 {} 的所有预约结果", dynastyId);
  }

  /** 清空指定官职的预约结果 */
  @Transactional
  public void clearPositionReservationResults(Long dynastyId, PositionType positionType) {
    validateEmperor(dynastyId);

    positionReservationRepository.deleteByDynastyIdAndPositionType(dynastyId, positionType);
    log.info("已清空王朝 {} 的 {} 官职预约结果", dynastyId, positionType);
  }

  /** 更新王朝信息 */
  @Transactional
  public Dynasty updateDynasty(Long dynastyId, UpdateDynastyRequest request) {
    validateEmperor(dynastyId);

    Dynasty dynasty =
        dynastyRepository.findById(dynastyId).orElseThrow(() -> new BusinessException("王朝不存在"));

    dynasty.setName(request.getName());
    Dynasty savedDynasty = dynastyRepository.save(dynasty);

    log.info("王朝 {} 信息已更新，新名称: {}", dynastyId, request.getName());
    return savedDynasty;
  }

  /** 删除王朝及所有相关数据 */
  @Transactional(rollbackFor = Exception.class)
  public void deleteDynasty(Long dynastyId) {
    validateEmperor(dynastyId);

    Dynasty dynasty =
        dynastyRepository.findById(dynastyId).orElseThrow(() -> new BusinessException("王朝不存在"));

    // 1. 删除所有官职预约记录（必须先删除，因为它们引用了DynastyPosition）
    positionReservationRepository.deleteByDynastyId(dynastyId);

    // 强制刷新以确保删除操作立即执行
    positionReservationRepository.flush();

    // 2. 删除所有官职配置
    dynastyPositionRepository.deleteByDynastyId(dynastyId);

    // 强制刷新以确保删除操作立即执行
    dynastyPositionRepository.flush();

    // 3. 清空所有成员的王朝关联（使用批量更新提高性能）
    gameAccountRepository.clearDynastyIdByDynastyId(dynastyId);

    // 强制刷新以确保更新操作立即执行
    gameAccountRepository.flush();

    // 4. 删除王朝本身
    dynastyRepository.delete(dynasty);

    log.info("王朝 {} ({}) 及所有相关数据已删除", dynasty.getName(), dynastyId);
  }

  /** 账号退出王朝 */
  @Transactional
  public GameAccount leaveDynasty(Long accountId) {
    Long userId = UserContext.getUserId();

    // 验证账号是否存在且属于当前用户
    GameAccount account =
        gameAccountRepository.findById(accountId).orElseThrow(() -> new BusinessException("账号不存在"));

    if (!account.getUserId().equals(userId)) {
      throw new BusinessException("无权操作此账号");
    }

    // 验证账号是否已加入王朝
    if (account.getDynastyId() == null) {
      throw new BusinessException("账号未加入任何王朝");
    }

    Long dynastyId = account.getDynastyId();

    // 退出王朝
    account.setDynastyId(null);
    GameAccount savedAccount = gameAccountRepository.save(account);
    positionReservationRepository.deleteByAccountId(accountId);

    log.info("账号 {} 已退出王朝 {}", account.getAccountName(), dynastyId);
    return savedAccount;
  }
}
