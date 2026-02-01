package com.app.gamehub.service;

import com.app.gamehub.dto.CreateGameAccountRequest;
import com.app.gamehub.dto.UpdateGameAccountRequest;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.AllianceApplicationRepository;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.BarbarianGroupRepository;
import com.app.gamehub.repository.CarriageQueueRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.repository.PositionReservationRepository;
import com.app.gamehub.repository.UserRepository;
import com.app.gamehub.repository.WarApplicationRepository;
import com.app.gamehub.repository.WarArrangementRepository;
import com.app.gamehub.service.ocr.OcrServiceManager;
import com.app.gamehub.util.UserContext;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class GameAccountService {

  @Autowired private GameAccountRepository gameAccountRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private AllianceRepository allianceRepository;

  @Autowired private PositionReservationRepository positionReservationRepository;

  @Autowired private WarApplicationRepository warApplicationRepository;

  @Autowired private WarArrangementRepository warArrangementRepository;

  @Autowired private AllianceApplicationRepository allianceApplicationRepository;

  @Autowired private BarbarianGroupRepository barbarianGroupRepository;

  @Autowired private CarriageQueueRepository carriageQueueRepository;

  @Autowired private OcrServiceManager ocrServiceManager;

  @Autowired private EntityManager entityManager;

  @Transactional
  public GameAccount createGameAccount(CreateGameAccountRequest request) {
    Long userId = UserContext.getUserId();
    // 验证用户是否存在
    if (!userRepository.existsById(userId)) {
      throw new BusinessException("用户不存在");
    }

    // 检查用户在该区是否已有2个账号
    long accountCount =
        gameAccountRepository.countByUserIdAndServerId(userId, request.getServerId());
    if (accountCount >= 2) {
      throw new BusinessException("每个用户在每个区最多只能创建2个账号");
    }

    Optional<GameAccount> gameAccount =
        gameAccountRepository.findByAccountNameAndServerIdAndUserId(
            request.getAccountName(), request.getServerId(), UserContext.getUserId());
    GameAccount account = new GameAccount();
    if (gameAccount.isPresent()) {
      account = gameAccount.get();
    }
    account.setUserId(userId);
    account.setServerId(request.getServerId());
    account.setAccountName(request.getAccountName());
    account.setPowerValue(request.getPowerValue());
    account.setDamageBonus(request.getDamageBonus());
    account.setTroopLevel(request.getTroopLevel());
    account.setRallyCapacity(request.getRallyCapacity());
    account.setTroopQuantity(request.getTroopQuantity());
    account.setInfantryDefense(request.getInfantryDefense());
    account.setInfantryHp(request.getInfantryHp());
    account.setArcherAttack(request.getArcherAttack());
    account.setArcherSiege(request.getArcherSiege());
    account.setLvbuStarLevel(request.getLvbuStarLevel());

    return gameAccountRepository.save(account);
  }

  @Transactional
  public GameAccount updateGameAccount(Long accountId, UpdateGameAccountRequest request) {
    Long userId = UserContext.getUserId();
    GameAccount account =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    // 验证权限：账号所有者或盟主可以更新
    boolean canUpdate = account.getUserId() != null && account.getUserId().equals(userId);
    if (!canUpdate && account.getAllianceId() != null) {
      Alliance alliance = allianceRepository.findById(account.getAllianceId()).orElse(null);
      canUpdate = alliance != null && alliance.getLeaderId().equals(userId);
    }

    if (!canUpdate) {
      throw new BusinessException("没有权限更新此账号");
    }

    // 检查是否修改了账号名称，如果修改了需要检查无主账号合并
    boolean accountNameChanged = false;
    String newAccountName = null;
    if (request.getAccountName() != null && !request.getAccountName().trim().isEmpty()) {
      newAccountName = request.getAccountName().trim();
      accountNameChanged = !newAccountName.equals(account.getAccountName());
    }

    // 如果修改了账号名称且账号属于联盟，检查是否有同名无主账号需要合并
    if (accountNameChanged && account.getAllianceId() != null) {
      Optional<GameAccount> unownedAccount =
          gameAccountRepository.findByAllianceIdAndAccountName(
              account.getAllianceId(), newAccountName);

      if (unownedAccount.isPresent() && unownedAccount.get().getUserId() == null) {
        // 找到同名的无主账号，进行合并
        log.info(
            "发现同名无主账号，开始合并：无主账号ID={}, 用户账号ID={}", unownedAccount.get().getId(), account.getId());

        // 合并无主账号到用户账号
        account = mergeUnownedAccountToUserAccount(account, unownedAccount.get());

        // 更新账号名称
        account.setAccountName(newAccountName);

        log.info("无主账号合并完成，账号名称已更新为: {}", newAccountName);
      } else {
        // 没有同名无主账号，正常更新账号名称
        account.setAccountName(newAccountName);
      }
    } else if (accountNameChanged) {
      // 账号不属于联盟或没有修改名称，正常更新
      account.setAccountName(newAccountName);
    }

    // 更新其他字段
    if (request.getDamageBonus() != null) {
      account.setDamageBonus(request.getDamageBonus());
    }
    if (request.getTroopLevel() != null) {
      account.setTroopLevel(request.getTroopLevel());
    }
    if (request.getRallyCapacity() != null) {
      account.setRallyCapacity(request.getRallyCapacity());
    }
    if (request.getTroopQuantity() != null) {
      account.setTroopQuantity(request.getTroopQuantity());
    }
    if (request.getInfantryDefense() != null) {
      account.setInfantryDefense(request.getInfantryDefense());
    }
    if (request.getInfantryHp() != null) {
      account.setInfantryHp(request.getInfantryHp());
    }
    if (request.getArcherAttack() != null) {
      account.setArcherAttack(request.getArcherAttack());
    }
    if (request.getArcherSiege() != null) {
      account.setArcherSiege(request.getArcherSiege());
    }
    if (request.getLvbuStarLevel() != null) {
      account.setLvbuStarLevel(request.getLvbuStarLevel());
    }

    return gameAccountRepository.save(account);
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteGameAccount(Long accountId) {
    Long userId = UserContext.getUserId();
    GameAccount account =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    // 验证是否为账号所有者
    if (account.getUserId() == null || !account.getUserId().equals(userId)) {
      throw new BusinessException("只能删除自己的账号");
    }

    log.info("开始删除游戏账号 ID: {}, 名称: {}", accountId, account.getAccountName());

    // 1. 删除所有官职预约记录（必须先删除，因为它们引用了账号）
    log.info("删除官职预约记录");
    positionReservationRepository.deleteByAccountId(accountId);
    positionReservationRepository.flush();

    // 2. 删除所有战事申请记录
    log.info("删除战事申请记录");
    warApplicationRepository.deleteAllByAccountId(accountId);
    warApplicationRepository.flush();

    // 3. 删除所有战事安排记录
    log.info("删除战事安排记录");
    warArrangementRepository.deleteAllByAccountId(accountId);
    warArrangementRepository.flush();

    // 4. 删除所有联盟申请记录
    log.info("删除联盟申请记录");
    allianceApplicationRepository.deleteAllByAccountId(accountId);
    allianceApplicationRepository.flush();

    // 5. 处理南蛮分组关联（如果有的话）
    if (account.getBarbarianGroupId() != null) {
      Long groupId = account.getBarbarianGroupId();
      log.info("处理南蛮分组关联");

      // 先清空账号的分组关联
      account.setBarbarianGroupId(null);
      gameAccountRepository.save(account);
      gameAccountRepository.flush();

      // 检查分组是否还有其他成员，如果没有则删除分组
      long memberCount = barbarianGroupRepository.countMembersByGroupId(groupId);
      if (memberCount == 0) {
        barbarianGroupRepository.deleteById(groupId);
        log.info("南蛮分组 {} 因无成员而被自动删除", groupId);
      }
    }

    // 6. 清空账号的联盟和王朝关联（如果有的话）
    if (account.getAllianceId() != null || account.getDynastyId() != null) {
      log.info("清空账号的联盟和王朝关联");
      account.setAllianceId(null);
      account.setMemberTier(null);
      account.setDynastyId(null);
      gameAccountRepository.save(account);
      gameAccountRepository.flush();
    }

    // 7. 删除联盟马车排队记录（如果有的话）
    log.info("删除联盟马车排队记录");
    carriageQueueRepository.deleteByAccountId(accountId);
    carriageQueueRepository.flush();

    // 7. 最后删除账号本身
    log.info("删除账号主体数据");
    gameAccountRepository.delete(account);

    log.info("游戏账号删除完成 ID: {}", accountId);
  }

  public List<GameAccount> getUserGameAccounts() {
    Long userId = UserContext.getUserId();
    return gameAccountRepository.findByUserIdOrderByServerIdDesc(userId);
  }

  public GameAccount getGameAccountById(Long accountId) {
    return gameAccountRepository
        .findById(accountId)
        .orElseThrow(() -> new BusinessException("游戏账号不存在"));
  }

  @Transactional
  public GameAccount updateStatsFromImage(Long accountId, MultipartFile file) {
    Long userId = UserContext.getUserId();
    GameAccount account =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    // 验证权限：账号所有者或所在联盟盟主可以更新
    boolean canUpdate = account.getUserId() != null && account.getUserId().equals(userId);
    if (!canUpdate && account.getAllianceId() != null) {
      Alliance alliance = allianceRepository.findById(account.getAllianceId()).orElse(null);
      canUpdate = alliance != null && alliance.getLeaderId().equals(userId);
    }
    if (!canUpdate) {
      throw new BusinessException("没有权限更新此账号的属性");
    }

    // 保存上传文件到临时文件并调用 OCR
    File tmp = null;
    try {
      // 获取原始文件扩展名
      String originalFilename = file.getOriginalFilename();
      String extension = ".png"; // 默认扩展名
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      }
      tmp = File.createTempFile("game_stat_upload_", extension);
      file.transferTo(tmp);

      Map<String, Integer> stats;

      // 使用OCR服务管理器（自动选择最优服务商）
      try {
        log.info("开始OCR识别图片");
        stats = ocrServiceManager.parseStats(tmp);
        log.info("OCR识别成功");
      } catch (Exception e) {
        log.error("OCR识别失败: {}", e.getMessage());
        throw new BusinessException("图片识别失败: " + e.getMessage());
      }

      if (stats.containsKey("步兵防御力")) account.setInfantryDefense(stats.get("步兵防御力"));
      if (stats.containsKey("步兵生命值")) account.setInfantryHp(stats.get("步兵生命值"));
      if (stats.containsKey("弓兵攻击力")) account.setArcherAttack(stats.get("弓兵攻击力"));
      if (stats.containsKey("弓兵破坏力")) account.setArcherSiege(stats.get("弓兵破坏力"));

      gameAccountRepository.save(account);

      return account;
    } catch (BusinessException be) {
      throw be;
    } catch (Exception ex) {
      throw new BusinessException("解析上传图片失败: " + ex.getMessage());
    } finally {
      // 删除临时文件
      if (tmp != null) {
        try {
          Files.deleteIfExists(tmp.toPath());
        } catch (Exception ignore) {
        }
      }
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
  protected GameAccount mergeUnownedAccountToUserAccount(
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

    // 设置联盟信息（如果用户账号还没有加入联盟）
    if (userAccount.getAllianceId() == null) {
      userAccount.setAllianceId(unownedAccount.getAllianceId());
    }

    // 2. 转移所有关联记录
    Long unownedAccountId = unownedAccount.getId();
    Long userAccountId = userAccount.getId();

    // 转移联盟申请记录
    allianceApplicationRepository.transferToAccount(unownedAccountId, userAccountId);
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
}
