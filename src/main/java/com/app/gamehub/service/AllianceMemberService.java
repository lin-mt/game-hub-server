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
import java.util.List;
import java.util.Optional;
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

  public AllianceMemberService(
      AllianceRepository allianceRepository,
      GameAccountRepository gameAccountRepository,
      AllianceApplicationRepository applicationRepository,
      WarApplicationRepository repository,
      WarArrangementRepository warArrangementRepository,
      BarbarianGroupRepository barbarianGroupRepository,
      PositionReservationRepository positionReservationRepository,
      CarriageQueueRepository carriageQueueRepository,
      EntityManager entityManager) {
    this.allianceRepository = allianceRepository;
    this.gameAccountRepository = gameAccountRepository;
    this.applicationRepository = applicationRepository;
    warApplicationRepository = repository;
    this.warArrangementRepository = warArrangementRepository;
    this.barbarianGroupRepository = barbarianGroupRepository;
    this.positionReservationRepository = positionReservationRepository;
    this.carriageQueueRepository = carriageQueueRepository;
    this.entityManager = entityManager;
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

    // 验证账号和联盟是否在同一个区
    if (!account.getServerId().equals(alliance.getServerId())) {
      throw new BusinessException("只能申请加入同一个区的联盟");
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
      GameAccount mergedAccount = mergeUnownedAccountToUserAccount(account, unownedAccount.get());
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
        GameAccount mergedAccount = mergeUnownedAccountToUserAccount(account, unownedAccount.get());
        log.info("用户 {} 通过同名无主账号加入联盟 {}", account.getUserId(), alliance.getId());
      } else {
        // 正常加入联盟流程
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
  public String bulkUpdateMembersFromText(Long allianceId, String rawText) {
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
    List<String> notFoundNames = new ArrayList<>();

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
      if (!m.find()) {
        // 可能是表头或无法解析的行，尝试从分隔符拆分
        String[] parts = trimmed.split("\\s+");
        // 期望至少 8 列：index,name,tier,furnace,power,week,total,contrib；姓名不包含空格，所以 parts[1] 是姓名
        if (parts.length < 8) continue;
        String name = parts[1];
        if (name.isBlank() || name.matches("^\\d+$")) continue;
        String tierStr = parts[2];
        String powerStr = parts[4]; // index(0),name(1),tier(2),furnace(3),power(4)
        int result = handleSingleEntry(allianceId, name, tierStr, powerStr, notFoundNames);
        if (result == 1) updatedCount++;
        else if (result == 2) createdCount++;
        continue;
      }

      String name = m.group(1).trim();
      String tierStr = m.group(2);
      String powerStr = m.group(3);

      // 姓名不会包含空格，若为空或是纯数字则跳过
      if (name.isBlank() || name.matches("^\\d+$")) continue;

      int result = handleSingleEntry(allianceId, name, tierStr, powerStr, notFoundNames);
      if (result == 1) updatedCount++;
      else if (result == 2) createdCount++;
    }

    StringBuilder resultMsg = new StringBuilder();
    resultMsg.append("已更新成员数: ").append(updatedCount);
    if (createdCount > 0) {
      resultMsg.append("，已创建无主账号数: ").append(createdCount);
    }
    resultMsg.append("。\n");

    return resultMsg.toString();
  }

  private int handleSingleEntry(
      Long allianceId, String name, String tierStr, String powerStr, List<String> notFoundNames) {
    if (name == null || name.isBlank()) return 0;

    // 解析阶级
    GameAccount.MemberTier tier = parseTier(tierStr);

    // 解析战力（移除逗号），并转为以万为单位的 Long
    String cleanedPower = powerStr.replaceAll(",", "");
    Long power = null;
    try {
      power = Long.parseLong(cleanedPower);
    } catch (NumberFormatException e) {
      // ignore
    }

    Optional<GameAccount> opt =
        gameAccountRepository.findByAllianceIdAndAccountName(allianceId, name);
    if (opt.isEmpty()) {
      // 账号不存在，创建一个不属于任何用户但属于联盟的账号
      GameAccount newAccount = createUnownedAllianceAccount(allianceId, name, tier, power);
      if (newAccount != null) {
        log.info("为联盟 {} 创建了无主账号: {}", allianceId, name);
        return 2; // 返回2表示创建了新账号
      } else {
        notFoundNames.add(name + " (原始战力:" + powerStr + ")");
        return 0; // 返回0表示失败
      }
    }

    GameAccount account = opt.get();
    if (tier != null) account.setMemberTier(tier);
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

    gameAccountRepository.save(account);
    return 1; // 返回1表示更新了现有账号
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
}
