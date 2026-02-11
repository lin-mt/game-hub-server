package com.app.gamehub.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.app.gamehub.dto.AllianceMemberExportDto;
import com.app.gamehub.dto.AllianceMemberSummaryDto;
import com.app.gamehub.dto.JoinAllianceRequest;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.AllianceApplication;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.AllianceApplicationRepository;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.BarbarianGroupRepository;
import com.app.gamehub.repository.CarriageQueueRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.repository.PositionReservationRepository;
import com.app.gamehub.repository.WarApplicationRepository;
import com.app.gamehub.repository.WarArrangementRepository;
import com.app.gamehub.util.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AllianceMemberService {

  private final AllianceRepository allianceRepository;
  private final GameAccountRepository gameAccountRepository;
  private final AllianceApplicationRepository applicationRepository;
  private final WarApplicationRepository warApplicationRepository;
  private final WarArrangementRepository warArrangementRepository;
  private final BarbarianGroupRepository barbarianGroupRepository;
  private final PositionReservationRepository positionReservationRepository;
  private final CarriageQueueRepository carriageQueueRepository;
  private final EntityManager entityManager;
  private final GameAccountService gameAccountService;

  public AllianceMemberService(
      AllianceRepository allianceRepository,
      GameAccountRepository gameAccountRepository,
      AllianceApplicationRepository applicationRepository,
      WarApplicationRepository repository,
      WarArrangementRepository warArrangementRepository,
      BarbarianGroupRepository barbarianGroupRepository,
      PositionReservationRepository positionReservationRepository,
      CarriageQueueRepository carriageQueueRepository,
      EntityManager entityManager,
      GameAccountService gameAccountService) {
    this.allianceRepository = allianceRepository;
    this.gameAccountRepository = gameAccountRepository;
    this.applicationRepository = applicationRepository;
    warApplicationRepository = repository;
    this.warArrangementRepository = warArrangementRepository;
    this.barbarianGroupRepository = barbarianGroupRepository;
    this.positionReservationRepository = positionReservationRepository;
    this.carriageQueueRepository = carriageQueueRepository;
    this.entityManager = entityManager;
    this.gameAccountService = gameAccountService;
  }

  @Transactional
  public AllianceApplication applyToJoinAlliance(JoinAllianceRequest request) {
    Long accountId = request.getAccountId();

    // 验证账号是否存在且属于当前用户
    GameAccount account =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    if (account.getUserId() == null || !account.getUserId().equals(UserContext.getUserId())) {
      throw new BusinessException("只能为自己的账号申请加入联盟");
    }

    // 查找联盟
    Alliance alliance =
        allianceRepository
            .findByCode(request.getAllianceCode().toUpperCase())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    // 验证账号是否已加入联盟
    if (account.getAllianceId() != null) {
      if (account.getAllianceId().equals(alliance.getId())) {
        return null;
      }
      throw new BusinessException("账号已加入联盟，请先退出当前联盟");
    }

    // 检查是否已有待处理的申请
    if (applicationRepository.existsByAccountIdAndStatus(
        accountId, AllianceApplication.ApplicationStatus.PENDING)) {
      throw new BusinessException("已有待处理的申请，请等待处理结果");
    }

    // 检查联盟中是否有同名的无主账号
    Optional<GameAccount> unownedAccount =
        gameAccountRepository.findByAllianceIdAndAccountName(
            alliance.getId(), account.getAccountName());

    if (unownedAccount.isPresent() && unownedAccount.get().getUserId() == null) {
      // 找到同名的无主账号，将其信息合并到用户账号中
      account.setServerId(alliance.getServerId());
      mergeUnownedAccountToUserAccount(account, unownedAccount.get());
      log.info("用户 {} 通过同名无主账号直接加入联盟 {}", UserContext.getUserId(), alliance.getId());
      return null;
    }

    // 创建申请
    AllianceApplication application = new AllianceApplication();
    application.setAccountId(accountId);
    application.setAllianceId(alliance.getId());

    // 检查是否需要审核
    if (alliance.getAllianceJoinApprovalRequired()) {
      // 需要审核，设置为待处理状态
      application.setStatus(AllianceApplication.ApplicationStatus.PENDING);
      log.info("账号 {} 申请加入联盟 {}，等待审核", accountId, alliance.getId());
    } else {
      // 不需要审核，直接通过
      application.setStatus(AllianceApplication.ApplicationStatus.APPROVED);
      application.setProcessedBy(alliance.getLeaderId()); // 系统自动处理，记录为盟主处理

      // 直接加入联盟
      account.setServerId(alliance.getServerId());
      account.setAllianceId(alliance.getId());
      account.setMemberTier(GameAccount.MemberTier.TIER_1); // 默认为一阶成员
      gameAccountRepository.save(account);

      log.info("账号 {} 自动加入联盟 {}（无需审核）", accountId, alliance.getId());
    }

    return applicationRepository.save(application);
  }

  @Transactional
  public AllianceApplication processApplication(Long applicationId, boolean approved) {
    Long userId = UserContext.getUserId();
    AllianceApplication application =
        applicationRepository
            .findById(applicationId)
            .orElseThrow(() -> new BusinessException("申请不存在"));

    // 验证是否为盟主
    Alliance alliance =
        allianceRepository
            .findById(application.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以处理申请");
    }

    // 验证申请状态
    if (application.getStatus() != AllianceApplication.ApplicationStatus.PENDING) {
      throw new BusinessException("申请已被处理");
    }

    if (approved) {
      // 通过申请，将账号加入联盟
      GameAccount account =
          gameAccountRepository
              .findById(application.getAccountId())
              .orElseThrow(() -> new BusinessException("游戏账号不存在"));

      // 检查联盟中是否有同名的无主账号
      Optional<GameAccount> unownedAccount =
          gameAccountRepository.findByAllianceIdAndAccountName(
              alliance.getId(), account.getAccountName());

      if (unownedAccount.isPresent() && unownedAccount.get().getUserId() == null) {
        // 找到同名的无主账号，将其信息合并到用户账号中
        account.setServerId(alliance.getServerId());
        mergeUnownedAccountToUserAccount(account, unownedAccount.get());
        log.info("用户 {} 通过同名无主账号加入联盟 {}", account.getUserId(), alliance.getId());
      } else {
        // 正常加入联盟流程
        account.setServerId(alliance.getServerId());
        account.setAllianceId(alliance.getId());
        account.setMemberTier(GameAccount.MemberTier.TIER_1); // 默认为一阶成员
        gameAccountRepository.save(account);
      }

      application.setStatus(AllianceApplication.ApplicationStatus.APPROVED);
    } else {
      application.setStatus(AllianceApplication.ApplicationStatus.REJECTED);
    }

    application.setProcessedBy(userId);
    return applicationRepository.save(application);
  }

  @Transactional
  public void removeMember(Long accountId) {
    Long userId = UserContext.getUserId();
    GameAccount account =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    if (account.getAllianceId() == null) {
      throw new BusinessException("账号未加入任何联盟");
    }

    // 验证是否为盟主
    Alliance alliance =
        allianceRepository
            .findById(account.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)
        && (account.getUserId() == null || !account.getUserId().equals(userId))) {
      throw new BusinessException("只有盟主或账号本人可以操作账号退出联盟");
    }

    // 处理南蛮分组关联（如果有的话）
    if (account.getBarbarianGroupId() != null) {
      Long groupId = account.getBarbarianGroupId();
      log.info("处理账号 {} 的南蛮分组关联", accountId);

      // 先清空账号的分组关联
      account.setBarbarianGroupId(null);

      // 检查分组是否还有其他成员，如果没有则删除分组
      long memberCount = barbarianGroupRepository.countMembersByGroupId(groupId);
      if (memberCount == 0) {
        barbarianGroupRepository.deleteById(groupId);
        log.info("南蛮分组 {} 因无成员而被自动删除", groupId);
      }
    }

    // 移除成员
    account.setAllianceId(null);
    account.setMemberTier(null);
    applicationRepository.deleteAllByAccountId(accountId);
    warApplicationRepository.deleteAllByAccountId(accountId);
    warArrangementRepository.deleteAllByAccountId(accountId);
    gameAccountRepository.save(account);
  }

  public List<AllianceApplication> getPendingApplications(Long allianceId) {
    Long userId = UserContext.getUserId();
    // 验证是否为盟主
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以查看申请列表");
    }

    return applicationRepository.findByAllianceIdAndStatusOrderByCreatedAtAsc(
        allianceId, AllianceApplication.ApplicationStatus.PENDING);
  }

  public List<AllianceApplication> getAccountApplications(Long accountId) {
    return applicationRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
  }

  // Changed return type to DTO list and map GameAccount -> DTO
  public List<AllianceMemberSummaryDto> getAllianceMembers(Long allianceId) {
    // 验证联盟是否存在
    allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    // 获取联盟成员列表（按战力值降序排列）
    List<GameAccount> accounts =
        gameAccountRepository.findByAllianceIdOrderByPowerValueDesc(allianceId);
    List<AllianceMemberSummaryDto> result = new ArrayList<>();
    for (GameAccount a : accounts) {
      result.add(toDto(a));
    }
    return result;
  }

  private AllianceMemberSummaryDto toDto(GameAccount a) {
    AllianceMemberSummaryDto d = new AllianceMemberSummaryDto();
    d.setId(a.getId());
    d.setUserId(a.getUserId());
    d.setMemberTier(a.getMemberTier() != null ? a.getMemberTier().name() : null);
    d.setAccountName(a.getAccountName());
    d.setLvbuStarLevel(a.getLvbuStarLevel());
    d.setDamageBonus(a.getDamageBonus());
    d.setPowerValue(a.getPowerValue());
    d.setTroopLevel(a.getTroopLevel());
    d.setInfantryDefense(a.getInfantryDefense());
    d.setInfantryHp(a.getInfantryHp());
    d.setArcherAttack(a.getArcherAttack());
    d.setArcherSiege(a.getArcherSiege());
    return d;
  }

  public void exportMembers(Long allianceId, HttpServletResponse response) throws IOException {
    // 验证联盟是否存在
    allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    List<GameAccount> accounts =
        gameAccountRepository.findByAllianceIdOrderByPowerValueDesc(allianceId);
    List<AllianceMemberExportDto> exportData =
        accounts.stream().map(this::toExportDto).collect(Collectors.toList());

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding("utf-8");
    String fileName = URLEncoder.encode("成员列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

    EasyExcel.write(response.getOutputStream(), AllianceMemberExportDto.class)
        .sheet("成员")
        .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
        .doWrite(exportData);
  }

  private AllianceMemberExportDto toExportDto(GameAccount a) {
    AllianceMemberExportDto d = new AllianceMemberExportDto();
    d.setServerId(a.getServerId());
    d.setAccountName(a.getAccountName());
    d.setRegistrationStatus(a.getUserId() != null ? "已注册" : "未注册");
    d.setMemberTier(a.getMemberTier() != null ? translateTier(a.getMemberTier()) : null);
    d.setLvbuStarLevel(a.getLvbuStarLevel());
    d.setDamageBonus(a.getDamageBonus());
    d.setPowerValue(a.getPowerValue());
    d.setTroopLevel(a.getTroopLevel());
    d.setRallyCapacity(a.getRallyCapacity());
    d.setTroopQuantity(a.getTroopQuantity());
    d.setInfantryDefense(a.getInfantryDefense());
    d.setInfantryHp(a.getInfantryHp());
    d.setArcherAttack(a.getArcherAttack());
    d.setArcherSiege(a.getArcherSiege());
    return d;
  }

  private String translateTier(GameAccount.MemberTier tier) {
    if (tier == null) return null;
    return switch (tier) {
      case TIER_1 -> "一阶";
      case TIER_2 -> "二阶";
      case TIER_3 -> "三阶";
      case TIER_4 -> "四阶";
      case TIER_5 -> "五阶";
    };
  }

  /**
   * 从传入的成员文本批量更新联盟成员的阶级和战力（战力单位：文本为实际数字，数据库以万为单位存储） 文本格式参考：成员信息.txt，每行包含：序号 成员名称 阶级 火炉等级 战力 ...
   * 本方法会根据成员名称在指定联盟中查找账号（精确匹配 accountName 字段），并更新 memberTier 和 powerValue
   * 如果账号不存在，则创建一个不属于任何用户但属于联盟的账号
   */
  @Transactional
  public String bulkUpdateMembersFromText(Long allianceId, String rawText, Boolean removeMissing) {
    // 验证联盟存在
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));
    // 仅允许盟主进行批量更新
    Long userId = UserContext.getUserId();
    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以批量更新成员信息");
    }

    String[] lines = rawText.split("\\r?\\n");
    int updatedCount = 0;
    int createdCount = 0;
    // 临时容器：解析后的导入成员（按名字去重，保证顺序）
    Map<String, ParsedMember> importedMap = new LinkedHashMap<>();

    // 现在姓名不会包含空格：使用更简单的正则
    // 格式：行号 + 多空格 + 姓名(非空格) + 多空格 + 阶位(数字) + 多空格 + 火炉等级 + 多空格 + 战力(任意位数数字, 允许千位分隔)
    Pattern linePattern =
        Pattern.compile(
            "^\\s*\\d+\\s+(\\S+)\\s+(\\d+)\\s+\\S+\\s+(\\d{1,}(?:,\\d{3})*|\\d+)\\b.*$");

    for (String line : lines) {
      if (line == null) continue;
      String trimmed = line.trim();
      if (trimmed.isEmpty()) continue;
      // 跳过表头行
      if (trimmed.contains("成员名称") || trimmed.contains("阶级") || trimmed.startsWith("成员")) continue;

      Matcher m = linePattern.matcher(line);
      String name;
      String tierStr;
      String powerStr;
      if (!m.find()) {
        // 尝试从空白分隔的列中解析
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 8) continue;
        name = parts[1].trim();
        if (name.isBlank() || name.matches("^\\d+$")) continue;
        tierStr = parts[2];
        powerStr = parts[4];
      } else {
        name = m.group(1).trim();
        tierStr = m.group(2);
        powerStr = m.group(3);
        if (name.isBlank() || name.matches("^\\d+$")) continue;
      }

      // parse tier
      GameAccount.MemberTier tier = parseTier(tierStr);
      // parse power
      String cleanedPower = powerStr.replaceAll(",", "");
      Long power = null;
      try {
        power = Long.parseLong(cleanedPower);
      } catch (Exception e) {
        // ignore malformed power, keep as null
      }

      // dedupe: keep first occurrence
      String key = name.trim();
      if (!importedMap.containsKey(key)) {
        importedMap.put(key, new ParsedMember(key, tier, power));
      }
    }

    // If no parsed members, return early
    if (importedMap.isEmpty()) {
      return "已更新成员数: 0。\n";
    }

    // Fetch all existing members of the alliance in one query
    List<GameAccount> existingAccounts = gameAccountRepository.findByAllianceId(allianceId);
    // existingAccounts already contains all members; we'll use it directly

    // Prepare lists for batch operations
    List<GameAccount> toUpdate = new ArrayList<>();
    List<GameAccount> toCreate = new ArrayList<>();
    List<Long> idsToDelete = new ArrayList<>();
    List<Long> idsToRemove = new ArrayList<>();

    // Determine updates and deletions
    for (GameAccount ga : existingAccounts) {
      String existingName = ga.getAccountName() != null ? ga.getAccountName().trim() : null;
      if (existingName == null) continue;
      ParsedMember parsed = importedMap.get(existingName);
      if (parsed != null) {
        // Update memberTier and power if present
        if (parsed.tier != null) ga.setMemberTier(parsed.tier);
        if (parsed.power != null) {
          long stored;
          if (parsed.power < 100000L) stored = 1L;
          else stored = parsed.power / 10000L;
          ga.setPowerValue(stored);
        }
        toUpdate.add(ga);
        // remove from map to mark as processed
        importedMap.remove(existingName);
      } else {
        // Not in imported list
        if (ga.getUserId() == null) {
          // 无主账号：删除
          idsToDelete.add(ga.getId());
        } else if (Boolean.TRUE.equals(removeMissing)) {
          // 如果选择移除不在名单中的成员
          // 已注册账号：移除出联盟
          idsToRemove.add(ga.getId());
        }
      }
    }

    // Remaining parsed entries in importedMap are creations
    for (ParsedMember pm : importedMap.values()) {
      GameAccount newAcc = new GameAccount();
      newAcc.setUserId(null);
      newAcc.setServerId(alliance.getServerId());
      newAcc.setAccountName(pm.name);
      newAcc.setAllianceId(allianceId);
      newAcc.setMemberTier(pm.tier);
      if (pm.power != null) {
        long stored;
        if (pm.power < 100000L) stored = 1L;
        else stored = pm.power / 10000L;
        newAcc.setPowerValue(stored);
      }
      toCreate.add(newAcc);
    }

    // Execute batch database operations
    if (!toUpdate.isEmpty()) {
      gameAccountRepository.saveAll(toUpdate);
      updatedCount = toUpdate.size();
    }
    if (!toCreate.isEmpty()) {
      gameAccountRepository.saveAll(toCreate);
      createdCount = toCreate.size();
    }
    if (!idsToDelete.isEmpty()) {
      // 无主账号：删除
      gameAccountService.deleteGameAccountsBatchAsSystem(idsToDelete);
    }
    if (!idsToRemove.isEmpty()) {
      // 已注册账号：批量移除出联盟
      removeMembersFromAllianceBatch(idsToRemove);
    }

    StringBuilder resultMsg = new StringBuilder();
    resultMsg.append("已更新成员数: ").append(updatedCount);
    if (createdCount > 0) {
      resultMsg.append("，已创建无主账号数: ").append(createdCount);
    }
    int removedCount = idsToRemove.size();
    if (removedCount > 0) {
      resultMsg.append("，已移除成员数: ").append(removedCount);
    }
    resultMsg.append("。\n");

    return resultMsg.toString();
  }

  /** 将成员移除出联盟（单个操作，循环调用会多次查询） */
  private void removeMemberFromAlliance(Long accountId) {
    GameAccount account =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    if (account.getAllianceId() == null) {
      return; // 已不在联盟中
    }

    // 处理南蛮分组关联（如果有的话）
    if (account.getBarbarianGroupId() != null) {
      Long groupId = account.getBarbarianGroupId();
      log.info("处理账号 {} 的南蛮分组关联", accountId);

      // 先清空账号的分组关联
      account.setBarbarianGroupId(null);

      // 检查分组是否还有其他成员，如果没有则删除分组
      long memberCount = barbarianGroupRepository.countMembersByGroupId(groupId);
      if (memberCount == 0) {
        barbarianGroupRepository.deleteById(groupId);
        log.info("南蛮分组 {} 因无成员而被自动删除", groupId);
      }
    }

    // 移除成员
    account.setAllianceId(null);
    account.setMemberTier(null);
    applicationRepository.deleteAllByAccountId(accountId);
    warApplicationRepository.deleteAllByAccountId(accountId);
    warArrangementRepository.deleteAllByAccountId(accountId);
    gameAccountRepository.save(account);
  }

  /** 批量移除成员出联盟 */
  private void removeMembersFromAllianceBatch(List<Long> accountIds) {
    if (accountIds.isEmpty()) {
      return;
    }
    // 批量查询账号
    List<GameAccount> accounts = gameAccountRepository.findAllById(accountIds);
    
    // 收集需要处理的南蛮分组ID
    Set<Long> groupIdsToCheck = new HashSet<>();
    
    for (GameAccount account : accounts) {
      if (account.getAllianceId() == null) {
        continue; // 已不在联盟中
      }
      
      // 处理南蛮分组关联
      if (account.getBarbarianGroupId() != null) {
        groupIdsToCheck.add(account.getBarbarianGroupId());
        account.setBarbarianGroupId(null);
      }
      
      // 移除成员
      account.setAllianceId(null);
      account.setMemberTier(null);
    }
    
    // 批量更新账号
    gameAccountRepository.saveAll(accounts);
    
    // 批量删除申请和战事安排
    applicationRepository.deleteAllByAccountIdIn(accountIds);
    warApplicationRepository.deleteAllByAccountIdIn(accountIds);
    warArrangementRepository.deleteAllByAccountIdIn(accountIds);
    
    // 检查并删除空南蛮分组
    for (Long groupId : groupIdsToCheck) {
      long memberCount = barbarianGroupRepository.countMembersByGroupId(groupId);
      if (memberCount == 0) {
        barbarianGroupRepository.deleteById(groupId);
        log.info("南蛮分组 {} 因无成员而被自动删除", groupId);
      }
    }
  }

  private GameAccount.MemberTier parseTier(String tierStr) {
    if (tierStr == null) return null;
    tierStr = tierStr.trim();
    try {
      int v = Integer.parseInt(tierStr);
      switch (v) {
        case 1:
          return GameAccount.MemberTier.TIER_1;
        case 2:
          return GameAccount.MemberTier.TIER_2;
        case 3:
          return GameAccount.MemberTier.TIER_3;
        case 4:
          return GameAccount.MemberTier.TIER_4;
        case 5:
          return GameAccount.MemberTier.TIER_5;
        default:
          return null;
      }
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * 创建一个不属于任何用户但属于联盟的账号
   *
   * @param allianceId 联盟ID
   * @param accountName 账号名称
   * @param tier 成员阶级
   * @param power 战力值
   * @return 创建的账号，如果创建失败返回null
   */
  private GameAccount createUnownedAllianceAccount(
      Long allianceId, String accountName, GameAccount.MemberTier tier, Long power) {
    try {
      // 获取联盟信息以确定服务器ID
      Alliance alliance =
          allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

      GameAccount account = new GameAccount();
      account.setUserId(null); // 不属于任何用户
      account.setServerId(alliance.getServerId());
      account.setAccountName(accountName);
      account.setAllianceId(allianceId);
      account.setMemberTier(tier);

      if (power != null) {
        long stored;
        // 如果原始战力少于6位数，则按照要求使用 1
        if (power < 100000L) {
          stored = 1L;
        } else {
          stored = power / 10000L; // 数据库存以万为单位
        }
        account.setPowerValue(stored);
      }

      return gameAccountRepository.save(account);
    } catch (Exception e) {
      log.error("创建无主联盟账号失败: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 将无主账号的信息合并到用户账号中，并转移所有关联记录
   *
   * @param userAccount 用户的原有账号
   * @param unownedAccount 无主账号
   * @return 更新后的用户账号
   */
  @Transactional
  public GameAccount mergeUnownedAccountToUserAccount(
      GameAccount userAccount, GameAccount unownedAccount) {
    log.info("开始合并无主账号 {} 到用户账号 {}", unownedAccount.getId(), userAccount.getId());

    // 1. 复制无主账号的非空字段到用户账号
    if (unownedAccount.getPowerValue() != null) {
      userAccount.setPowerValue(unownedAccount.getPowerValue());
    }
    if (unownedAccount.getDamageBonus() != null) {
      userAccount.setDamageBonus(unownedAccount.getDamageBonus());
    }
    if (unownedAccount.getTroopLevel() != null) {
      userAccount.setTroopLevel(unownedAccount.getTroopLevel());
    }
    if (unownedAccount.getRallyCapacity() != null) {
      userAccount.setRallyCapacity(unownedAccount.getRallyCapacity());
    }
    if (unownedAccount.getTroopQuantity() != null) {
      userAccount.setTroopQuantity(unownedAccount.getTroopQuantity());
    }
    if (unownedAccount.getInfantryDefense() != null) {
      userAccount.setInfantryDefense(unownedAccount.getInfantryDefense());
    }
    if (unownedAccount.getInfantryHp() != null) {
      userAccount.setInfantryHp(unownedAccount.getInfantryHp());
    }
    if (unownedAccount.getArcherAttack() != null) {
      userAccount.setArcherAttack(unownedAccount.getArcherAttack());
    }
    if (unownedAccount.getArcherSiege() != null) {
      userAccount.setArcherSiege(unownedAccount.getArcherSiege());
    }
    if (unownedAccount.getLvbuStarLevel() != null) {
      userAccount.setLvbuStarLevel(unownedAccount.getLvbuStarLevel());
    }
    if (unownedAccount.getMemberTier() != null) {
      userAccount.setMemberTier(unownedAccount.getMemberTier());
    }
    if (unownedAccount.getBarbarianGroupId() != null) {
      userAccount.setBarbarianGroupId(unownedAccount.getBarbarianGroupId());
    }

    // 设置联盟信息
    if (unownedAccount.getServerId() != null) {
      userAccount.setServerId(unownedAccount.getServerId());
    }
    userAccount.setAllianceId(unownedAccount.getAllianceId());

    // 2. 转移所有关联记录
    Long unownedAccountId = unownedAccount.getId();
    Long userAccountId = userAccount.getId();

    // 转移联盟申请记录
    applicationRepository.transferToAccount(unownedAccountId, userAccountId);
    entityManager.flush(); // 强制执行SQL
    log.info("已转移联盟申请记录");

    // 转移战事申请记录
    warApplicationRepository.transferToAccount(unownedAccountId, userAccountId);
    entityManager.flush(); // 强制执行SQL
    log.info("已转移战事申请记录");

    // 转移战事安排记录
    warArrangementRepository.transferToAccount(unownedAccountId, userAccountId);
    entityManager.flush(); // 强制执行SQL
    log.info("已转移战事安排记录");

    // 转移官职预约记录
    positionReservationRepository.transferToAccount(unownedAccountId, userAccountId);
    entityManager.flush(); // 强制执行SQL
    log.info("已转移官职预约记录");

    // 转移马车排队记录
    carriageQueueRepository.transferToAccount(unownedAccountId, userAccountId);
    entityManager.flush(); // 强制执行SQL
    log.info("已转移马车排队记录");

    // 3. 保存更新后的用户账号
    GameAccount savedUserAccount = gameAccountRepository.save(userAccount);

    // 4. 删除无主账号
    gameAccountRepository.delete(unownedAccount);
    entityManager.flush(); // 强制执行SQL

    // 5. 清除EntityManager缓存，确保后续查询从数据库获取最新数据
    entityManager.clear();
    log.info("已删除无主账号 {}", unownedAccountId);

    log.info("账号合并完成，用户账号 {} 已获得无主账号的所有信息和关联记录", userAccountId);
    return savedUserAccount;
  }

  /**
   * 获取联盟中的无主账号列表
   *
   * @param allianceId 联盟ID
   * @return 无主账号列表
   */
  public List<GameAccount> getUnownedAccounts(Long allianceId) {
    // 验证联盟是否存在
    allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    // 获取联盟中的无主账号列表（按账号名称排序）
    return gameAccountRepository.findByAllianceIdAndUserIdIsNull(allianceId);
  }

  // Helper container for parsed import rows
  private static class ParsedMember {
    String name;
    GameAccount.MemberTier tier;
    Long power; // original parsed power (actual number), will be converted to stored value later

    ParsedMember(String name, GameAccount.MemberTier tier, Long power) {
      this.name = name;
      this.tier = tier;
      this.power = power;
    }
  }
}
