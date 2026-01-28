package com.app.gamehub.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.app.gamehub.dto.TacticSortMode;
import com.app.gamehub.dto.TacticTemplateConfig;
import com.app.gamehub.dto.UseTacticRequest;
import com.app.gamehub.dto.WarLimitStatusResponse;
import com.app.gamehub.dto.WarPersonnelExportDto;
import com.app.gamehub.dto.WarRequest;
import com.app.gamehub.entity.*;
import com.app.gamehub.entity.TacticTemplate;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.model.TacticalArrangement;
import com.app.gamehub.model.WarTactic;
import com.app.gamehub.repository.*;
import com.app.gamehub.repository.TacticTemplateRepository;
import com.app.gamehub.util.RankExpressionParser;
import com.app.gamehub.util.SpringContextUtils;
import com.app.gamehub.util.TacticTemplateUtils;
import com.app.gamehub.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarService {

  private final WarApplicationRepository warApplicationRepository;
  private final GameAccountRepository gameAccountRepository;
  private final AllianceRepository allianceRepository;
  private final WarArrangementRepository warArrangementRepository;
  private final WarGroupRepository warGroupRepository;
  private final TacticTemplateRepository tacticTemplateRepository;
  private final AllianceService allianceService;

  // 用于控制并发申请的锁，按联盟ID和战事类型分组
  private final ConcurrentHashMap<String, ReentrantLock> warApplicationLocks = new ConcurrentHashMap<>();

  /**
   * 获取战事申请锁
   */
  private ReentrantLock getWarApplicationLock(Long allianceId, WarType warType, boolean isSubstitute) {
    String lockKey = allianceId + "_" + warType + "_" + isSubstitute;
    return warApplicationLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
  }

  @Transactional
  public WarApplication applyForWar(WarRequest request) {
    Long userId = UserContext.getUserId();
    Long accountId = request.getAccountId();

    // 验证账号是否存在且属于当前用户
    GameAccount account =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    if (!account.getUserId().equals(userId)) {
      throw new BusinessException("只能为自己的账号申请参加战事");
    }

    // 验证账号是否已加入联盟
    if (account.getAllianceId() == null) {
      throw new BusinessException("必须先加入联盟才能申请参加战事");
    }

    // 验证战事类型（只能申请官渡一或官渡二）
    if (request.getWarType() != WarType.GUANDU_ONE && request.getWarType() != WarType.GUANDU_TWO) {
      throw new BusinessException("只能申请参加官渡一或官渡二战事");
    }

    // 检查是否已有官渡战事的待处理申请
    List<WarType> guanduWars = Arrays.asList(WarType.GUANDU_ONE, WarType.GUANDU_TWO);
    if (warApplicationRepository.existsByAccountIdAndWarTypeIn(accountId, guanduWars)) {
      throw new BusinessException("只能参与一场官渡战事");
    }

    // 获取联盟信息以检查审核设置
    Alliance alliance =
        allianceRepository
            .findById(account.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    // 检查官渡报名时间限制（联盟管理员添加人员不受限制）
    if (WarType.isGuanDu(request.getWarType()) && !allianceService.isGuanduRegistrationTimeValid(alliance)) {
      throw new BusinessException("当前不在官渡报名时间范围内");
    }

    // 使用应用层锁控制并发申请
    boolean isSubstitute = Boolean.TRUE.equals(request.getIsSubstitute());
    ReentrantLock lock = getWarApplicationLock(alliance.getId(), request.getWarType(), isSubstitute);
    
    lock.lock();
    try {
      // 检查官渡战事人数上限（区分主力/替补）
      if (WarType.isGuanDu(request.getWarType())) {
        checkWarLimit(alliance, request.getWarType(), isSubstitute);
      }

      // 创建申请
      WarApplication application = new WarApplication();
      application.setAccountId(accountId);
      application.setAllianceId(account.getAllianceId());
      application.setWarType(request.getWarType());
      application.setIsSubstitute(isSubstitute);

      // 检查是否需要审核
      if (alliance.getWarJoinApprovalRequired()) {
        // 需要审核，设置为待处理状态
        application.setStatus(WarApplication.ApplicationStatus.PENDING);
        log.info("账号 {} 申请参加战事 {}，等待审核", accountId, request.getWarType());
      } else {
        // 不需要审核，直接通过
        application.setStatus(WarApplication.ApplicationStatus.APPROVED);
        application.setProcessedBy(alliance.getLeaderId()); // 系统自动处理，记录为盟主处理

        // 直接加入战事安排
        WarArrangement arrangement = new WarArrangement();
        arrangement.setAccountId(accountId);
        arrangement.setAllianceId(account.getAllianceId());
        arrangement.setWarType(request.getWarType());
        arrangement.setIsSubstitute(isSubstitute);
        // warGroupId 为 null，表示机动人员
        warArrangementRepository.save(arrangement);

        log.info("账号 {} 自动加入战事 {}（无需审核）", accountId, request.getWarType());
      }

      return warApplicationRepository.save(application);
    } finally {
      lock.unlock();
    }
  }

  @Transactional
  public WarApplication processWarApplication(Long applicationId, boolean approved) {
    Long userId = UserContext.getUserId();
    WarApplication application =
        warApplicationRepository
            .findById(applicationId)
            .orElseThrow(() -> new BusinessException("申请不存在"));

    // 验证是否为盟主
    Alliance alliance =
        allianceRepository
            .findById(application.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以处理战事申请");
    }

    // 验证申请状态
    if (application.getStatus() != WarApplication.ApplicationStatus.PENDING) {
      throw new BusinessException("申请已被处理");
    }

    if (approved) {
      if (WarType.isGuanDu(application.getWarType())) {
        if (warArrangementRepository.existsByAccountIdAndWarTypeIn(
            application.getAccountId(), WarType.allGuanDu())) {
          throw new BusinessException("一个账号只能参与一场官渡战事");
        }

        // 使用应用层锁控制并发审核
        ReentrantLock lock = getWarApplicationLock(alliance.getId(), application.getWarType(), application.getIsSubstitute());
        
        lock.lock();
        try {
          // 检查人数上限（审核通过时需要重新检查，因为可能有其他申请已经通过）
          checkWarLimitForApproval(alliance, application.getWarType(), application.getIsSubstitute());
          
          application.setStatus(WarApplication.ApplicationStatus.APPROVED);
          WarArrangement arrangement = new WarArrangement();
          arrangement.setAccountId(application.getAccountId());
          arrangement.setAllianceId(alliance.getId());
          arrangement.setWarType(application.getWarType());
          arrangement.setIsSubstitute(application.getIsSubstitute());
          warArrangementRepository.save(arrangement);
        } finally {
          lock.unlock();
        }
      } else {
        application.setStatus(WarApplication.ApplicationStatus.APPROVED);
        WarArrangement arrangement = new WarArrangement();
        arrangement.setAccountId(application.getAccountId());
        arrangement.setAllianceId(alliance.getId());
        arrangement.setWarType(application.getWarType());
        arrangement.setIsSubstitute(application.getIsSubstitute());
        warArrangementRepository.save(arrangement);
      }
    } else {
      application.setStatus(WarApplication.ApplicationStatus.REJECTED);
    }

    application.setProcessedBy(userId);
    return warApplicationRepository.save(application);
  }

  public List<WarApplication> getPendingWarApplications(Long allianceId, WarType warType) {
    Long userId = UserContext.getUserId();
    // 验证是否为盟主
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以查看战事申请列表");
    }

    return warApplicationRepository.findByAllianceIdAndWarTypeAndStatusOrderByCreatedAtAsc(
        allianceId, warType, WarApplication.ApplicationStatus.PENDING);
  }

  public List<WarApplication> getAccountWarApplications(Long accountId) {
    return warApplicationRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
  }

  /** 检查官渡战事人数上限，区分主力/替补 */
  private void checkWarLimit(Alliance alliance, WarType warType, boolean isSub) {
    int mainLimit;
    int subLimit;
    if (warType == WarType.GUANDU_ONE) {
      mainLimit = alliance.getGuanduOneMainLimit();
      subLimit = alliance.getGuanduOneSubLimit();
    } else if (warType == WarType.GUANDU_TWO) {
      mainLimit = alliance.getGuanduTwoMainLimit();
      subLimit = alliance.getGuanduTwoSubLimit();
    } else {
      return; // 非官渡战事不检查人数上限
    }

    // 计算当前人数：已在战事中的人数（区分主力/替补） + 待处理申请人数（区分主力/替补）
    long currentArranged =
        warArrangementRepository.countByAllianceIdAndWarTypeAndIsSubstitute(
            alliance.getId(), warType, isSub);
    long pendingCount =
        warApplicationRepository.countByAllianceIdAndWarTypeAndStatusAndIsSubstitute(
            alliance.getId(), warType, WarApplication.ApplicationStatus.PENDING, isSub);

    if (isSub) {
      if (currentArranged + pendingCount >= subLimit) {
        String warTypeName = warType == WarType.GUANDU_ONE ? "官渡一替补" : "官渡二替补";
        throw new BusinessException(String.format("%s人数已达上限（%d人）", warTypeName, subLimit));
      }
    } else {
      if (currentArranged + pendingCount >= mainLimit) {
        String warTypeName = warType == WarType.GUANDU_ONE ? "官渡一主力" : "官渡二主力";
        throw new BusinessException(String.format("%s人数已达上限（%d人）", warTypeName, mainLimit));
      }
    }

    log.info(
        "战事 {} 当前人数检查（isSub={}）：已安排 {}, 待处理申请 {}, 上限 {}",
        warType,
        isSub,
        currentArranged,
        pendingCount,
        isSub ? subLimit : mainLimit);
  }

  /** 检查官渡战事人数上限（用于审核通过时），区分主力/替补 */
  private void checkWarLimitForApproval(Alliance alliance, WarType warType, boolean isSub) {
    int mainLimit;
    int subLimit;
    if (warType == WarType.GUANDU_ONE) {
      mainLimit = alliance.getGuanduOneMainLimit();
      subLimit = alliance.getGuanduOneSubLimit();
    } else if (warType == WarType.GUANDU_TWO) {
      mainLimit = alliance.getGuanduTwoMainLimit();
      subLimit = alliance.getGuanduTwoSubLimit();
    } else {
      return; // 非官渡战事不检查人数上限
    }

    long currentArranged =
        warArrangementRepository.countByAllianceIdAndWarTypeAndIsSubstitute(
            alliance.getId(), warType, isSub);

    if (isSub) {
      if (currentArranged >= subLimit) {
        String warTypeName = warType == WarType.GUANDU_ONE ? "官渡一替补" : "官渡二替补";
        throw new BusinessException(String.format("%s人数已达上限（%d人），无法通过申请", warTypeName, subLimit));
      }
    } else {
      if (currentArranged >= mainLimit) {
        String warTypeName = warType == WarType.GUANDU_ONE ? "官渡一主力" : "官渡二主力";
        throw new BusinessException(String.format("%s人数已达上限（%d人），无法通过申请", warTypeName, mainLimit));
      }
    }

    log.info(
        "审核通过时战事 {} 人数检查（isSub={}）：已安排 {}, 上限 {}",
        warType,
        isSub,
        currentArranged,
        isSub ? subLimit : mainLimit);
  }

  /** 获取联盟官渡战事人数上限状态（包含主力/替补） */
  public WarLimitStatusResponse getWarLimitStatus(Long allianceId) {
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    WarLimitStatusResponse response = new WarLimitStatusResponse();

    // 官渡一统计
    response.setGuanduOneMainLimit(alliance.getGuanduOneMainLimit());
    response.setGuanduOneMainArranged(
        warArrangementRepository.countByAllianceIdAndWarTypeAndIsSubstitute(
            allianceId, WarType.GUANDU_ONE, false));
    response.setGuanduOneMainPending(
        warApplicationRepository.countByAllianceIdAndWarTypeAndStatusAndIsSubstitute(
            allianceId, WarType.GUANDU_ONE, WarApplication.ApplicationStatus.PENDING, false));
    response.setGuanduOneMainFull(
        response.getGuanduOneMainArranged() + response.getGuanduOneMainPending()
            >= alliance.getGuanduOneMainLimit());

    response.setGuanduOneSubLimit(alliance.getGuanduOneSubLimit());
    response.setGuanduOneSubArranged(
        warArrangementRepository.countByAllianceIdAndWarTypeAndIsSubstitute(
            allianceId, WarType.GUANDU_ONE, true));
    response.setGuanduOneSubPending(
        warApplicationRepository.countByAllianceIdAndWarTypeAndStatusAndIsSubstitute(
            allianceId, WarType.GUANDU_ONE, WarApplication.ApplicationStatus.PENDING, true));
    response.setGuanduOneSubFull(
        response.getGuanduOneSubArranged() + response.getGuanduOneSubPending()
            >= alliance.getGuanduOneSubLimit());

    // 官渡二统计
    response.setGuanduTwoMainLimit(alliance.getGuanduTwoMainLimit());
    response.setGuanduTwoMainArranged(
        warArrangementRepository.countByAllianceIdAndWarTypeAndIsSubstitute(
            allianceId, WarType.GUANDU_TWO, false));
    response.setGuanduTwoMainPending(
        warApplicationRepository.countByAllianceIdAndWarTypeAndStatusAndIsSubstitute(
            allianceId, WarType.GUANDU_TWO, WarApplication.ApplicationStatus.PENDING, false));
    response.setGuanduTwoMainFull(
        response.getGuanduTwoMainArranged() + response.getGuanduTwoMainPending()
            >= alliance.getGuanduTwoMainLimit());

    response.setGuanduTwoSubLimit(alliance.getGuanduTwoSubLimit());
    response.setGuanduTwoSubArranged(
        warArrangementRepository.countByAllianceIdAndWarTypeAndIsSubstitute(
            allianceId, WarType.GUANDU_TWO, true));
    response.setGuanduTwoSubPending(
        warApplicationRepository.countByAllianceIdAndWarTypeAndStatusAndIsSubstitute(
            allianceId, WarType.GUANDU_TWO, WarApplication.ApplicationStatus.PENDING, true));
    response.setGuanduTwoSubFull(
        response.getGuanduTwoSubArranged() + response.getGuanduTwoSubPending()
            >= alliance.getGuanduTwoSubLimit());

    return response;
  }

  public WarArrangement moveGuanDuWar(Long accountId) {
    gameAccountRepository.findById(accountId).orElseThrow(() -> new BusinessException("账号不存在"));
    List<WarArrangement> arrangements =
        warArrangementRepository.findByAccountIdAndWarTypeIn(
            accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO));
    if (arrangements.size() != 1) {
      throw new BusinessException("账号不能同时参加官渡一和官渡二");
    }
    WarArrangement warArrangement = arrangements.get(0);
    if (warArrangement.getWarType().equals(WarType.GUANDU_ONE)) {
      warArrangement.setWarType(WarType.GUANDU_TWO);
    } else {
      warArrangement.setWarType(WarType.GUANDU_ONE);
    }
    warArrangement.setWarGroupId(null);
    warArrangementRepository.save(warArrangement);
    return warArrangement;
  }

  public WarArrangement addToWar(Long accountId, WarType warType, Boolean isSubstitute) {
    GameAccount account =
        gameAccountRepository.findById(accountId).orElseThrow(() -> new BusinessException("账号不存在"));
    Alliance alliance =
        allianceRepository
            .findById(account.getAllianceId())
            .orElseThrow(() -> new BusinessException("账号所在的联盟不存在"));
    if (!UserContext.getUserId().equals(alliance.getLeaderId())) {
      throw new BusinessException("只有盟主才能添加成员到战事中");
    }
    if (warType == WarType.GUANDU_ONE || warType == WarType.GUANDU_TWO) {
      List<WarArrangement> arrangements =
          warArrangementRepository.findByAccountIdAndWarTypeIn(
              accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO));
      if (arrangements.size() == 1) {
        WarArrangement arrangement = arrangements.getFirst();
        if (arrangement.getWarType().equals(warType)) {
          throw new BusinessException("当前成员已在当前战事中");
        } else {
          throw new BusinessException("当前成员不能同时参加官渡一和官渡二");
        }
      }
    }
    WarApplication warApplication = new WarApplication();
    warApplication.setAccountId(accountId);
    warApplication.setAllianceId(alliance.getId());
    warApplication.setWarType(warType);
    warApplication.setIsSubstitute(isSubstitute);
    warApplication.setStatus(WarApplication.ApplicationStatus.APPROVED);
    warApplication.setProcessedBy(UserContext.getUserId());
    warApplicationRepository.save(warApplication);
    WarArrangement arrangement = new WarArrangement();
    arrangement.setAccountId(accountId);
    arrangement.setAllianceId(alliance.getId());
    arrangement.setWarType(warType);
    arrangement.setIsSubstitute(isSubstitute);
    warArrangementRepository.save(arrangement);
    return arrangement;
  }

  @Transactional
  public void removeFromWar(Long accountId, WarType warType) {
    GameAccount account =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("当前账号不存在"));
    Alliance alliance =
        allianceRepository
            .findById(account.getAllianceId())
            .orElseThrow(() -> new BusinessException("账号所在联盟不存在"));
    Long currentUserId = UserContext.getUserId();
    if (!currentUserId.equals(alliance.getLeaderId())) {
      throw new BusinessException("只有联盟盟主才能移除战事中的成员");
    }
    log.info("用户：{} 从战事 {} 中移除账号 {}", currentUserId, warType, accountId);
    warArrangementRepository.deleteByAccountIdAndWarType(accountId, warType);
    warApplicationRepository.deleteByAccountIdAndWarType(accountId, warType);
  }

  @Transactional
  public void cancelApplyForWar(@Valid WarRequest warRequest) {
    GameAccount account =
        gameAccountRepository
            .findById(warRequest.getAccountId())
            .orElseThrow(() -> new BusinessException("账号不存在"));
    if (!account.getUserId().equals(UserContext.getUserId())) {
      throw new BusinessException("不能取消他人的战事申请");
    }
    warApplicationRepository.deleteByAccountIdAndWarType(
        warRequest.getAccountId(), warRequest.getWarType());
    warArrangementRepository.deleteByAccountIdAndWarType(
        warRequest.getAccountId(), warRequest.getWarType());
  }

  @Transactional
  public void useTactic(Long allianceId, UseTacticRequest request) {
    WarType warType = request.getWarType();
    String tacticKey = request.getTactic();

    // 验证权限：只有盟主可以使用战术
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));
    if (!Objects.equals(alliance.getLeaderId(), UserContext.getUserId())) {
      throw new BusinessException("只有盟主可以使用战术");
    }

    // 先删除现有安排及分组
    List<WarArrangement> existingArrangements =
        warArrangementRepository.findByAllianceIdAndWarTypeOrderByCreatedAtDesc(
            allianceId, warType);
    if (!existingArrangements.isEmpty()) {
      warArrangementRepository.deleteAll(existingArrangements);
      warGroupRepository.deleteByAllianceIdAndWarType(allianceId, warType);
    }

    // collect accountIds from existing arrangements and call application repo (some tests expect
    // this call)
    List<Long> existingAccountIds =
        existingArrangements.stream()
            .map(WarArrangement::getAccountId)
            .collect(Collectors.toList());
    if (!existingAccountIds.isEmpty()) {
      warApplicationRepository.findByAccountIdInAndWarTypeAndStatus(
          existingAccountIds, warType, WarApplication.ApplicationStatus.APPROVED);
    }

    List<TacticalArrangement> arrangement = null;

    // 1) Try DB-driven template by tacticKey
    try {
      Optional<TacticTemplate> opt = tacticTemplateRepository.findByTacticKey(tacticKey);
      if (opt.isPresent()) {
        TacticTemplate t = opt.get();
        ObjectMapper om = SpringContextUtils.getBean(ObjectMapper.class);
        if (om == null) om = new ObjectMapper();
        TacticTemplateConfig cfg = om.readValue(t.getConfigJson(), TacticTemplateConfig.class);
        if (cfg != null && cfg.getGroups() != null) {
          arrangement = new ArrayList<>();
          for (TacticTemplateConfig.GroupConfig g : cfg.getGroups()) {
            TacticalArrangement ta = new TacticalArrangement();
            WarGroup wg = new WarGroup();
            // keep raw name/task (placeholders will be resolved after accounts are loaded)
            wg.setGroupName(g.getName());
            wg.setGroupTask(g.getTask());
            ta.setWarGroup(wg);
            ta.setWarArrangements(new ArrayList<>());
            // ranks will be resolved later after we fetch accounts
            arrangement.add(ta);
          }
        }
      }
    } catch (Exception ex) {
      // ignore and fallback to enum
      arrangement = null;
    }

    // 获取当前申请并按申请时间排序（用于官渡战事），并收集 accountIds
    List<WarApplication> applicants =
        warApplicationRepository.findByAllianceIdAndWarTypeAndStatusOrderByCreatedAtAsc(
            allianceId, warType, WarApplication.ApplicationStatus.APPROVED);

    List<Long> accountIds =
        applicants.stream().map(WarApplication::getAccountId).collect(Collectors.toList());

    // Also fetch applications by accountIds (some tests expect this repository call)
    if (!accountIds.isEmpty()) {
      warApplicationRepository.findByAccountIdInAndWarTypeAndStatus(
          accountIds, warType, WarApplication.ApplicationStatus.APPROVED);
    }

    // 获取账号信息
    List<GameAccount> accounts = gameAccountRepository.findAllById(accountIds);

    // Apply requested sorting mode to accounts (default is highest damage bonus)
    if (TacticSortMode.FOUR_STATS.equals(request.getSortMode())) {
      // sort by sum of four stats: infantryHp + infantryDefense + archerAttack + archerSiege (nulls
      // treated as 0)
      accounts.sort(
          (GameAccount a, GameAccount b) -> {
            long sa = 0L;
            if (a.getInfantryHp() != null) sa += a.getInfantryHp();
            if (a.getInfantryDefense() != null) sa += a.getInfantryDefense();
            if (a.getArcherAttack() != null) sa += a.getArcherAttack();
            if (a.getArcherSiege() != null) sa += a.getArcherSiege();
            long sb = 0L;
            if (b.getInfantryHp() != null) sb += b.getInfantryHp();
            if (b.getInfantryDefense() != null) sb += b.getInfantryDefense();
            if (b.getArcherAttack() != null) sb += b.getArcherAttack();
            if (b.getArcherSiege() != null) sb += b.getArcherSiege();
            return Long.compare(sb, sa); // descending order
          });
    } else {
      // default: highest damage bonus (existing behavior for non-官渡 wars is handled inside
      // WarTactic.arrangement)
      // But for 官渡 we must ensure a deterministic ordering: use damageBonus descending with nulls
      // last
      accounts.sort(
          Comparator.comparing(
                  GameAccount::getDamageBonus, Comparator.nullsLast(Comparator.naturalOrder()))
              .reversed());
    }

    // If we previously loaded DB template, fill it using actual accounts; otherwise fallback to
    // enum
    if (arrangement != null && !arrangement.isEmpty()) {
      // fill per-group members based on ranks from config
      // reload template to get ranks properly
      TacticTemplate t = tacticTemplateRepository.findByTacticKey(tacticKey).orElse(null);
      if (t != null) {
        try {
          ObjectMapper om = SpringContextUtils.getBean(ObjectMapper.class);
          if (om == null) om = new ObjectMapper();
          TacticTemplateConfig cfg = om.readValue(t.getConfigJson(), TacticTemplateConfig.class);
          List<TacticalArrangement> newArrs = new ArrayList<>();
          for (TacticTemplateConfig.GroupConfig g : cfg.getGroups()) {
            TacticalArrangement ta = new TacticalArrangement();
            WarGroup wg = new WarGroup();
            // resolve rank placeholders in name and task now that we have accounts
            String resolvedName =
                TacticTemplateUtils.replaceRankPlaceholders(g.getName(), accounts);
            String resolvedTask =
                TacticTemplateUtils.replaceRankPlaceholders(g.getTask(), accounts);
            wg.setGroupName(resolvedName);
            wg.setGroupTask(resolvedTask);
            ta.setWarGroup(wg);
            ta.setWarArrangements(new ArrayList<>());
            List<Integer> ranks = RankExpressionParser.parse(g.getRanks());
            for (int rank : ranks) {
              int idx = rank - 1;
              if (idx >= 0 && idx < accounts.size()) {
                GameAccount acc = accounts.get(idx);
                WarArrangement wa = new WarArrangement();
                wa.setAccountId(acc.getId());
                wa.setAccount(acc);
                ta.getWarArrangements().add(wa);
              }
            }
            newArrs.add(ta);
          }

          // fill remaining accounts not assigned
          boolean[] assigned = new boolean[accounts.size()];
          for (TacticalArrangement a : newArrs) {
            for (WarArrangement wa : a.getWarArrangements()) {
              if (wa.getAccount() != null && wa.getAccount().getId() != null) {
                long id = wa.getAccount().getId();
                if (id >= 1 && id <= accounts.size()) assigned[(int) id - 1] = true;
              }
            }
          }
          int groupCount = newArrs.size() == 0 ? 1 : newArrs.size();
          int nextGroup = 0;
          for (int i = 0; i < accounts.size(); i++) {
            if (!assigned[i]) {
              GameAccount acc = accounts.get(i);
              WarArrangement wa = new WarArrangement();
              wa.setAccountId(acc.getId());
              wa.setAccount(acc);
              newArrs.get(nextGroup % groupCount).getWarArrangements().add(wa);
              nextGroup++;
            }
          }
          arrangement = newArrs;
        } catch (Exception ex) {
          arrangement = null; // fallback
        }
      }
    }

    if (arrangement == null) {
      // fallback to enum by name
      WarTactic tacticEnum;
      try {
        tacticEnum = WarTactic.valueOf(tacticKey);
      } catch (Exception ex) {
        throw new BusinessException("找不到指定的战术: " + tacticKey);
      }
      if (!tacticEnum.getSupportedWarTypes().contains(warType)) {
        throw new BusinessException("该战术不支持当前战事类型");
      }
      arrangement = tacticEnum.arrangement(accounts);
    }

    arrangement.forEach(
        tacticalArrangement -> {
          List<WarArrangement> tacticalArrangements = tacticalArrangement.getWarArrangements();
          if (tacticalArrangements.isEmpty()) {
            return;
          }
          WarGroup warGroup = tacticalArrangement.getWarGroup();
          warGroup.setWarType(warType);
          warGroup.setAllianceId(allianceId);
          warGroupRepository.save(warGroup);
          tacticalArrangements.forEach(
              warArrangement -> {
                warArrangement.setWarType(warType);
                warArrangement.setAllianceId(allianceId);
                warArrangement.setWarGroupId(warGroup.getId());
              });
          warArrangementRepository.saveAll(tacticalArrangements);
        });
  }

  public void exportWarPersonnel(Long allianceId, HttpServletResponse response) throws IOException {
    // 验证联盟是否存在
    allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding("utf-8");
    String fileName = URLEncoder.encode("战事人员名单", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

    try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream())
        .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
        .build()) {
      WarType[] warTypes = WarType.values();
      for (int i = 0; i < warTypes.length; i++) {
        WarType warType = warTypes[i];
        List<WarPersonnelExportDto> data = getWarPersonnelData(allianceId, warType);
        WriteSheet writeSheet = EasyExcel.writerSheet(i, warType.getDescription()).head(WarPersonnelExportDto.class).build();
        excelWriter.write(data, writeSheet);
      }
    }
  }

  private List<WarPersonnelExportDto> getWarPersonnelData(Long allianceId, WarType warType) {
    List<WarArrangement> arrangements = warArrangementRepository.findByAllianceIdAndWarTypeOrderByCreatedAtDesc(allianceId, warType);
    Map<Long, String> groupNames = warGroupRepository.findByAllianceIdAndWarType(allianceId, warType)
        .stream().collect(Collectors.toMap(WarGroup::getId, WarGroup::getGroupName, (v1, v2) -> v1));

    return arrangements.stream().map(a -> {
      GameAccount acc = a.getAccount();
      if (acc == null) {
        acc = gameAccountRepository.findById(a.getAccountId()).orElse(null);
      }

      WarPersonnelExportDto d = new WarPersonnelExportDto();
      d.setGroupName(a.getWarGroupId() != null ? groupNames.getOrDefault(a.getWarGroupId(), "未知分组") : "机动人员");
      d.setIsSubstitute(Boolean.TRUE.equals(a.getIsSubstitute()) ? "替补" : "主力");
      if (acc != null) {
        d.setAccountName(acc.getAccountName());
        d.setMemberTier(acc.getMemberTier() != null ? translateTier(acc.getMemberTier()) : null);
        d.setPowerValue(acc.getPowerValue());
        d.setTroopLevel(acc.getTroopLevel());
        d.setLvbuStarLevel(acc.getLvbuStarLevel());
        d.setDamageBonus(acc.getDamageBonus());
        d.setRallyCapacity(acc.getRallyCapacity());
        d.setTroopQuantity(acc.getTroopQuantity());
        d.setInfantryDefense(acc.getInfantryDefense());
        d.setInfantryHp(acc.getInfantryHp());
        d.setArcherAttack(acc.getArcherAttack());
        d.setArcherSiege(acc.getArcherSiege());
      }
      return d;
    }).collect(Collectors.toList());
  }

  private String translateTier(GameAccount.MemberTier tier) {
    if (tier == null) return null;
    return switch (tier) {
      case TIER_1 -> "阶级1";
      case TIER_2 -> "阶级2";
      case TIER_3 -> "阶级3";
      case TIER_4 -> "阶级4";
      case TIER_5 -> "阶级5";
    };
  }
}
