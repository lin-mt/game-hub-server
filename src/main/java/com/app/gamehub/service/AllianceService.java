package com.app.gamehub.service;

import com.app.gamehub.dto.CreateAllianceRequest;
import com.app.gamehub.dto.TransferAllianceRequest;
import com.app.gamehub.dto.UpdateAllianceApprovalSettingsRequest;
import com.app.gamehub.dto.UpdateAllianceRequest;
import com.app.gamehub.dto.UpdateGuanduRegistrationTimeRequest;
import com.app.gamehub.dto.UpdateWarLimitsRequest;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.AllianceApplicationRepository;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.BarbarianGroupRepository;
import com.app.gamehub.repository.CarriageQueueRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.repository.UserRepository;
import com.app.gamehub.repository.WarApplicationRepository;
import com.app.gamehub.repository.WarArrangementRepository;
import com.app.gamehub.repository.WarGroupRepository;
import com.app.gamehub.util.AllianceCodeGenerator;
import com.app.gamehub.util.UserContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AllianceService {

  private final AllianceRepository allianceRepository;
  private final UserRepository userRepository;
  private final GameAccountRepository gameAccountRepository;
  private final AllianceApplicationRepository allianceApplicationRepository;
  private final WarApplicationRepository warApplicationRepository;
  private final WarArrangementRepository warArrangementRepository;
  private final WarGroupRepository warGroupRepository;
  private final BarbarianGroupRepository barbarianGroupRepository;
  private final CarriageQueueRepository carriageQueueRepository;
  private final AllianceCodeGenerator codeGenerator;

  public AllianceService(
      AllianceRepository allianceRepository,
      UserRepository userRepository,
      GameAccountRepository gameAccountRepository,
      AllianceApplicationRepository allianceApplicationRepository,
      WarApplicationRepository warApplicationRepository,
      WarArrangementRepository warArrangementRepository,
      WarGroupRepository warGroupRepository,
      BarbarianGroupRepository barbarianGroupRepository,
      CarriageQueueRepository carriageQueueRepository,
      AllianceCodeGenerator codeGenerator) {
    this.allianceRepository = allianceRepository;
    this.userRepository = userRepository;
    this.gameAccountRepository = gameAccountRepository;
    this.allianceApplicationRepository = allianceApplicationRepository;
    this.warApplicationRepository = warApplicationRepository;
    this.warArrangementRepository = warArrangementRepository;
    this.warGroupRepository = warGroupRepository;
    this.barbarianGroupRepository = barbarianGroupRepository;
    this.carriageQueueRepository = carriageQueueRepository;
    this.codeGenerator = codeGenerator;
  }

  @Transactional
  public Alliance createAlliance(CreateAllianceRequest request) {
    Long userId = UserContext.getUserId();
    // 验证用户是否存在
    if (!userRepository.existsById(userId)) {
      throw new BusinessException("用户不存在");
    }

    // 生成唯一的联盟编码
    String code;
    do {
      code = codeGenerator.generateCode();
    } while (allianceRepository.existsByCode(code));

    Alliance alliance = new Alliance();
    alliance.setName(request.getName());
    alliance.setCode(code);
    alliance.setServerId(request.getServerId());
    alliance.setAllianceJoinApprovalRequired(request.getAllianceJoinApprovalRequired());
    alliance.setWarJoinApprovalRequired(request.getWarJoinApprovalRequired());
    // set new main/sub limits
    alliance.setGuanduOneMainLimit(request.getGuanduOneMainLimit());
    alliance.setGuanduOneSubLimit(request.getGuanduOneSubLimit());
    alliance.setGuanduTwoMainLimit(request.getGuanduTwoMainLimit());
    alliance.setGuanduTwoSubLimit(request.getGuanduTwoSubLimit());
    alliance.setGuanduReminder(request.getGuanduReminder());
    alliance.setYyChannelId(request.getYyChannelId());
    alliance.setTencentMeetingCode(request.getTencentMeetingCode());
    alliance.setTencentMeetingPassword(request.getTencentMeetingPassword());
    alliance.setLeaderId(userId);

    return allianceRepository.save(alliance);
  }

  @Transactional
  public Alliance updateAlliance(Long allianceId, UpdateAllianceRequest request) {
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    // 验证是否为盟主
    if (!alliance.getLeaderId().equals(UserContext.getUserId())) {
      throw new BusinessException("只有盟主可以更新联盟信息");
    }

    if (request.getName() != null && !request.getName().trim().isEmpty()) {
      alliance.setName(request.getName().trim());
    }

    if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
      String newCode = request.getCode().trim().toUpperCase();
      if (!alliance.getCode().equals(newCode)) {
        if (allianceRepository.existsByCode(newCode)) {
          throw new BusinessException("联盟编码已存在");
        }
        alliance.setCode(newCode);
      }
    }

    if (request.getGuanduReminder() != null) {
      alliance.setGuanduReminder(request.getGuanduReminder().trim());
    }

    if (request.getYyChannelId() != null) {
      alliance.setYyChannelId(request.getYyChannelId().trim());
    }

    if (request.getTencentMeetingCode() != null) {
      alliance.setTencentMeetingCode(request.getTencentMeetingCode().trim());
    }

    if (request.getTencentMeetingPassword() != null) {
      alliance.setTencentMeetingPassword(request.getTencentMeetingPassword().trim());
    }

    return allianceRepository.save(alliance);
  }

  @Transactional
  public Alliance updateAllianceApprovalSettings(UpdateAllianceApprovalSettingsRequest request) {
    Alliance alliance =
        allianceRepository
            .findById(request.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    // 验证是否为盟主
    if (!alliance.getLeaderId().equals(UserContext.getUserId())) {
      throw new BusinessException("只有盟主可以更新联盟审核设置");
    }

    if (request.getAllianceJoinApprovalRequired() != null) {
      alliance.setAllianceJoinApprovalRequired(request.getAllianceJoinApprovalRequired());
    }

    if (request.getWarJoinApprovalRequired() != null) {
      alliance.setWarJoinApprovalRequired(request.getWarJoinApprovalRequired());
    }

    Alliance savedAlliance = allianceRepository.save(alliance);
    log.info(
        "联盟 {} 审核设置已更新，加入联盟需审核: {}, 参加官渡需审核: {}",
        alliance.getId(),
        savedAlliance.getAllianceJoinApprovalRequired(),
        savedAlliance.getWarJoinApprovalRequired());

    return savedAlliance;
  }

  @Transactional
  public Alliance updateWarLimits(UpdateWarLimitsRequest request) {
    Alliance alliance =
        allianceRepository
            .findById(request.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    // 验证是否为盟主
    if (!alliance.getLeaderId().equals(UserContext.getUserId())) {
      throw new BusinessException("只有盟主可以更新官渡战事人数上限");
    }

    if (request.getGuanduOneMainLimit() != null) {
      alliance.setGuanduOneMainLimit(request.getGuanduOneMainLimit());
    }

    if (request.getGuanduOneSubLimit() != null) {
      alliance.setGuanduOneSubLimit(request.getGuanduOneSubLimit());
    }

    if (request.getGuanduTwoMainLimit() != null) {
      alliance.setGuanduTwoMainLimit(request.getGuanduTwoMainLimit());
    }

    if (request.getGuanduTwoSubLimit() != null) {
      alliance.setGuanduTwoSubLimit(request.getGuanduTwoSubLimit());
    }

    Alliance savedAlliance = allianceRepository.save(alliance);
    log.info(
        "联盟 {} 官渡战事人数上限已更新，官渡一主力: {}, 官渡一替补: {}, 官渡二主力: {}, 官渡二替补: {}",
        alliance.getId(),
        savedAlliance.getGuanduOneMainLimit(),
        savedAlliance.getGuanduOneSubLimit(),
        savedAlliance.getGuanduTwoMainLimit(),
        savedAlliance.getGuanduTwoSubLimit());

    return savedAlliance;
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteAlliance(Long allianceId) {
    Long userId = UserContext.getUserId();
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    // 验证是否为盟主
    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以删除联盟");
    }

    log.info("开始删除联盟 ID: {}, 名称: {}", allianceId, alliance.getName());

    // 1. 删除所有战事申请数据（必须先删除，因为它们引用了联盟）
    log.info("删除战事申请数据");
    warApplicationRepository.deleteAllByAllianceId(allianceId);
    warApplicationRepository.flush();

    // 2. 删除所有战事安排数据
    log.info("删除战事安排数据");
    warArrangementRepository.deleteByAllianceId(allianceId);
    warArrangementRepository.flush();

    // 3. 删除所有战事分组数据
    log.info("删除战事分组数据");
    warGroupRepository.deleteByAllianceId(allianceId);
    warGroupRepository.flush();

    // 4. 删除所有申请加入联盟的数据
    log.info("删除联盟申请数据");
    allianceApplicationRepository.deleteAllByAllianceId(allianceId);
    allianceApplicationRepository.flush();

    // 5. 删除所有南蛮分组数据
    log.info("删除南蛮分组数据");
    barbarianGroupRepository.deleteByAllianceId(allianceId);
    barbarianGroupRepository.flush();

    // 6. 删除所有马车排队数据
    log.info("删除马车排队数据");
    carriageQueueRepository.deleteByAllianceId(allianceId);
    carriageQueueRepository.flush();

    // 7. 批量清空所有联盟成员的联盟关联（使用批量更新提高性能）
    log.info("清空联盟成员关联");
    gameAccountRepository.clearAllianceIdByAllianceId(allianceId);
    gameAccountRepository.flush();

    // 8. 最后删除联盟本身
    log.info("删除联盟主体数据");
    allianceRepository.delete(alliance);

    log.info("联盟删除完成 ID: {}", allianceId);
  }

  @Transactional
  public Alliance transferAlliance(Long allianceId, TransferAllianceRequest request) {
    Long userId = UserContext.getUserId();
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    // 验证是否为盟主
    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以转交联盟");
    }

    GameAccount gameAccount =
        gameAccountRepository
            .findById(request.getAccountId())
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));
    Long newLeaderId = gameAccount.getUserId();

    // 验证新盟主是否存在
    if (!userRepository.existsById(newLeaderId)) {
      throw new BusinessException("新盟主用户不存在");
    }

    // 验证新盟主是否为联盟成员
    boolean isNewLeaderMember =
        gameAccountRepository.existsByUserIdAndServerIdAndAllianceIdIsNotNull(
            newLeaderId, alliance.getServerId());
    if (!isNewLeaderMember) {
      throw new BusinessException("新盟主必须是联盟成员");
    }

    alliance.setLeaderId(newLeaderId);
    return allianceRepository.save(alliance);
  }

  public List<Alliance> getUserAlliances() {
    Long userId = UserContext.getUserId();
    return allianceRepository.findByLeaderIdOrderByServerIdDesc(userId);
  }

  public Alliance getAllianceById(Long allianceId) {
    return allianceRepository
        .findById(allianceId)
        .orElseThrow(() -> new BusinessException("联盟不存在"));
  }

  public Alliance getAllianceByCode(String code) {
    return allianceRepository
        .findByCode(code.toUpperCase())
        .orElseThrow(() -> new BusinessException("联盟不存在"));
  }

  @Transactional
  public Alliance updateGuanduRegistrationTime(Long allianceId, UpdateGuanduRegistrationTimeRequest request) {
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    // 验证是否为盟主
    if (!alliance.getLeaderId().equals(UserContext.getUserId())) {
      throw new BusinessException("只有盟主可以设置官渡报名时间");
    }

    // 验证时间设置的合法性
    validateGuanduRegistrationTime(request);

    alliance.setGuanduRegistrationStartDay(request.getStartDay());
    alliance.setGuanduRegistrationStartMinute(request.getStartMinute());
    alliance.setGuanduRegistrationEndDay(request.getEndDay());
    alliance.setGuanduRegistrationEndMinute(request.getEndMinute());

    Alliance savedAlliance = allianceRepository.save(alliance);
    log.info(
        "联盟 {} 官渡报名时间已更新，开始时间: 星期{} {}:{:02d}, 结束时间: 星期{} {}:{:02d}",
        alliance.getId(),
        request.getStartDay(),
        request.getStartMinute() / 60,
        request.getStartMinute() % 60,
        request.getEndDay(),
        request.getEndMinute() / 60,
        request.getEndMinute() % 60);

    return savedAlliance;
  }

  @Transactional
  public Alliance clearGuanduRegistrationTime(Long allianceId) {
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    // 验证是否为盟主
    if (!alliance.getLeaderId().equals(UserContext.getUserId())) {
      throw new BusinessException("只有盟主可以清除官渡报名时间设置");
    }

    alliance.setGuanduRegistrationStartDay(null);
    alliance.setGuanduRegistrationStartMinute(null);
    alliance.setGuanduRegistrationEndDay(null);
    alliance.setGuanduRegistrationEndMinute(null);

    Alliance savedAlliance = allianceRepository.save(alliance);
    log.info("联盟 {} 官渡报名时间设置已清除", alliance.getId());

    return savedAlliance;
  }

  /**
   * 验证官渡报名时间设置的合法性
   */
  private void validateGuanduRegistrationTime(UpdateGuanduRegistrationTimeRequest request) {
    // 开始时间必须在星期一之后，并且每天都必须在1:00以后
    if (request.getStartDay() < 1 || request.getStartDay() > 7) {
      throw new BusinessException("开始时间星期设置无效");
    }
    
    // 每天的开始时间都必须在1:00之后
    if (request.getStartMinute() < 60) {
      throw new BusinessException("开始时间必须在每天1:00之后");
    }
    
    // 开始时间不能是星期日
    if (request.getStartDay() == 7) {
      throw new BusinessException("开始时间不能设置在星期日");
    }

    // 结束时间必须在星期六10:00之前
    if (request.getEndDay() == 6 && request.getEndMinute() >= 600) {
      throw new BusinessException("结束时间必须在星期六10:00之前");
    }
    if (request.getEndDay() == 7) {
      throw new BusinessException("结束时间必须在星期六10:00之前");
    }

    // 验证时间范围的合理性（开始时间必须早于结束时间）
    int startTotalMinutes = (request.getStartDay() - 1) * 1440 + request.getStartMinute();
    int endTotalMinutes = (request.getEndDay() - 1) * 1440 + request.getEndMinute();
    
    if (startTotalMinutes >= endTotalMinutes) {
      throw new BusinessException("开始时间必须早于结束时间");
    }
  }

  /**
   * 检查当前时间是否在官渡报名时间范围内
   */
  public boolean isGuanduRegistrationTimeValid(Alliance alliance) {
    // 如果没有设置报名时间，则始终允许报名
    if (alliance.getGuanduRegistrationStartDay() == null || 
        alliance.getGuanduRegistrationStartMinute() == null ||
        alliance.getGuanduRegistrationEndDay() == null || 
        alliance.getGuanduRegistrationEndMinute() == null) {
      return true;
    }

    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    int currentDayOfWeek = now.getDayOfWeek().getValue(); // 1=星期一, 7=星期日
    int currentMinute = now.getHour() * 60 + now.getMinute();

    int currentTotalMinutes = (currentDayOfWeek - 1) * 1440 + currentMinute;
    int startTotalMinutes = (alliance.getGuanduRegistrationStartDay() - 1) * 1440 + alliance.getGuanduRegistrationStartMinute();
    int endTotalMinutes = (alliance.getGuanduRegistrationEndDay() - 1) * 1440 + alliance.getGuanduRegistrationEndMinute();

    return currentTotalMinutes >= startTotalMinutes && currentTotalMinutes < endTotalMinutes;
  }
}
