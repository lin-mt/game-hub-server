package com.app.gamehub.service;

import com.app.gamehub.dto.*;
import com.app.gamehub.entity.*;
import com.app.gamehub.enums.ActivityType;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.repository.WarApplicationRepository;
import com.app.gamehub.repository.WarArrangementRepository;
import com.app.gamehub.repository.WarGroupRepository;
import com.app.gamehub.util.UserContext;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WarGroupService {

  private final WarGroupRepository warGroupRepository;
  private final WarArrangementRepository warArrangementRepository;
  private final AllianceRepository allianceRepository;
  private final GameAccountRepository gameAccountRepository;
  private final WarApplicationRepository warApplicationRepository;
  private final AllianceNotificationService allianceNotificationService;

  public WarGroupService(
      GameAccountRepository gameAccountRepository,
      AllianceRepository allianceRepository,
      WarArrangementRepository warArrangementRepository,
      WarGroupRepository warGroupRepository,
      WarApplicationRepository warApplicationRepository,
      AllianceNotificationService allianceNotificationService) {
    this.gameAccountRepository = gameAccountRepository;
    this.allianceRepository = allianceRepository;
    this.warArrangementRepository = warArrangementRepository;
    this.warGroupRepository = warGroupRepository;
    this.warApplicationRepository = warApplicationRepository;
    this.allianceNotificationService = allianceNotificationService;
  }

  @Transactional
  public WarGroup createWarGroup(CreateWarGroupRequest request) {
    Long userId = UserContext.getUserId();
    Long allianceId = request.getAllianceId();

    // 验证是否为盟主
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以创建战事分组");
    }

    WarGroup warGroup = new WarGroup();
    warGroup.setAllianceId(allianceId);
    warGroup.setWarType(request.getWarType());
    warGroup.setGroupName(request.getGroupName());
    warGroup.setGroupTask(request.getGroupTask());

    return warGroupRepository.save(warGroup);
  }

  @Transactional
  public WarGroup updateWarGroup(Long groupId, UpdateWarGroupRequest request) {
    Long userId = UserContext.getUserId();
    WarGroup warGroup =
        warGroupRepository.findById(groupId).orElseThrow(() -> new BusinessException("战事分组不存在"));

    // 验证是否为盟主
    Alliance alliance =
        allianceRepository
            .findById(warGroup.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以更新战事分组");
    }

    if (request.getGroupName() != null && !request.getGroupName().trim().isEmpty()) {
      warGroup.setGroupName(request.getGroupName().trim());
    }
    if (request.getGroupTask() != null) {
      warGroup.setGroupTask(request.getGroupTask().trim());
    }

    return warGroupRepository.save(warGroup);
  }

  @Transactional
  public void deleteWarGroup(Long groupId) {
    Long userId = UserContext.getUserId();
    WarGroup warGroup =
        warGroupRepository.findById(groupId).orElseThrow(() -> new BusinessException("战事分组不存在"));

    // 验证是否为盟主
    Alliance alliance =
        allianceRepository
            .findById(warGroup.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以删除战事分组");
    }

    // 删除分组前，将分组内的成员移动到机动人员
    List<WarArrangement> arrangements =
        warArrangementRepository.findByWarGroupIdOrderByCreatedAtAsc(groupId);
    if (!arrangements.isEmpty()) {
      for (WarArrangement arrangement : arrangements) {
        arrangement.setWarGroup(null);
        arrangement.setWarGroupId(null);
      }
      warArrangementRepository.saveAll(arrangements);
    }
    warGroupRepository.delete(warGroup);
  }

  @Transactional
  public WarArrangement arrangeMember(ArrangeMemberRequest request) {
    Long userId = UserContext.getUserId();
    Long accountId = request.getAccountId();

    // 验证账号是否存在
    GameAccount account =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    if (account.getAllianceId() == null) {
      throw new BusinessException("账号未加入联盟");
    }

    // 验证是否为盟主
    Alliance alliance =
        allianceRepository
            .findById(account.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以安排成员");
    }

    // 验证分组是否存在（如果指定了分组）
    Long warGroupId = null;
    if (request.getWarGroupId() != null) {
      warGroupId = request.getWarGroupId();
      WarGroup warGroup =
          warGroupRepository
              .findById(warGroupId)
              .orElseThrow(() -> new BusinessException("战事分组不存在"));

      if (!warGroup.getAllianceId().equals(alliance.getId())
          || !warGroup.getWarType().equals(request.getWarType())) {
        throw new BusinessException("分组不属于当前联盟或战事类型不匹配");
      }
    }

    // 查找或创建安排记录
    List<WarArrangement> existingArrangements =
        warArrangementRepository.findByAllianceIdAndWarTypeOrderByCreatedAtDesc(
            alliance.getId(), request.getWarType());

    WarArrangement arrangement =
        existingArrangements.stream()
            .filter(arr -> arr.getAccountId().equals(accountId))
            .findFirst()
            .orElse(null);

    if (arrangement == null) {
      arrangement = new WarArrangement();
      arrangement.setAccountId(accountId);
      arrangement.setAllianceId(alliance.getId());
      arrangement.setWarType(request.getWarType());
    }

    arrangement.setWarGroupId(warGroupId);
    return warArrangementRepository.save(arrangement);
  }

  @Transactional
  public void clearWarArrangements(Long allianceId, WarType warType) {
    Long userId = UserContext.getUserId();
    // 验证是否为盟主
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以清空战事安排");
    }

    warApplicationRepository.deleteAllByAllianceId(allianceId);
    warArrangementRepository.deleteByAllianceIdAndWarType(allianceId, warType);
  }

  @Transactional
  public void clearWarArrangementsWithNotification(ClearWarArrangementsRequest request) {
    Long userId = UserContext.getUserId();
    // 验证是否为盟主
    Alliance alliance =
        allianceRepository
            .findById(request.getAllianceId())
            .orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以清空战事安排");
    }

    // 如果需要发送通知
    if (request.getSendNotification() != null && request.getSendNotification()) {
      clearWarArrangements(request.getAllianceId(), WarType.GUANDU_ONE);
      clearWarArrangements(request.getAllianceId(), WarType.GUANDU_TWO);
      sendGuanduNotification(request, alliance);
    } else {
      clearWarArrangements(request.getAllianceId(), request.getWarType());
    }
  }

  private void sendGuanduNotification(ClearWarArrangementsRequest request, Alliance alliance) {
    // 只有官渡一和官渡二才发送官渡报名通知
    if (request.getWarType() != WarType.GUANDU_ONE && request.getWarType() != WarType.GUANDU_TWO) {
      return;
    }

    try {
      SendAllianceNotificationRequest notificationRequest = new SendAllianceNotificationRequest();
      notificationRequest.setAllianceId(request.getAllianceId());
      notificationRequest.setActivityType(ActivityType.GUAN_DU_BAO_MING);
      notificationRequest.setStartTime(LocalDateTime.now());

      String remark = request.getNotificationRemark();
      if (remark == null || remark.trim().isEmpty()) {
        remark = "官渡战事重新开放报名，请及时申请参加";
      }
      notificationRequest.setRemark(remark);

      allianceNotificationService.sendAllianceNotification(notificationRequest);
      log.info(
          "清空战事安排后发送官渡报名通知成功，联盟: {}, 战事: {}",
          alliance.getName(),
          request.getWarType().getDescription());
    } catch (Exception e) {
      log.error(
          "发送官渡报名通知失败，联盟: {}, 战事: {}, 错误: {}",
          alliance.getName(),
          request.getWarType().getDescription(),
          e.getMessage());
    }
  }

  public List<WarGroup> getWarGroups(Long allianceId, WarType warType) {
    return warGroupRepository.findByAllianceIdAndWarTypeOrderByCreatedAtAsc(allianceId, warType);
  }

  public List<WarArrangement> getWarArrangements(Long allianceId, WarType warType) {
    return warArrangementRepository.findByAllianceIdAndWarTypeOrderByCreatedAtDesc(
        allianceId, warType);
  }

  public List<WarArrangement> getAllWarArrangements(Long allianceId) {
    return warArrangementRepository.findByAllianceIdOrderByCreatedAtAsc(allianceId);
  }

  public WarArrangementResponse getWarArrangementDetail(Long allianceId, WarType warType) {
    // 验证联盟是否存在
    allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    WarArrangementResponse response = new WarArrangementResponse();
    response.setAllianceId(allianceId);
    response.setWarType(warType);

    // 获取该联盟该战事类型的所有分组
    List<WarGroup> warGroups =
        warGroupRepository.findByAllianceIdAndWarTypeOrderByCreatedAtAsc(allianceId, warType);

    // 获取该联盟该战事类型的所有人员安排
    List<WarArrangement> arrangements =
        warArrangementRepository.findByAllianceIdAndWarTypeOrderByCreatedAtDesc(
            allianceId, warType);

    // 获取所有相关的账号信息
    List<Long> accountIds =
        arrangements.stream().map(WarArrangement::getAccountId).collect(Collectors.toList());

    final Map<Long, GameAccount> accountMap;
    final Map<Long, LocalDateTime> applicationTimeMap = new HashMap<>();
    final Map<Long, Boolean> isSubstituteMap = new HashMap<>();

    if (!accountIds.isEmpty()) {
      List<GameAccount> accounts = gameAccountRepository.findAllById(accountIds);
      accountMap =
          accounts.stream().collect(Collectors.toMap(GameAccount::getId, account -> account));

      // 如果是官渡战事，获取申请时间及替补标志
      if (WarType.isGuanDu(warType)) {
        List<WarApplication> applications =
            warApplicationRepository.findByAccountIdInAndWarTypeAndStatus(
                accountIds, warType, WarApplication.ApplicationStatus.APPROVED);
        for (WarApplication app : applications) {
          applicationTimeMap.put(app.getAccountId(), app.getCreatedAt());
          isSubstituteMap.put(app.getAccountId(), Boolean.TRUE.equals(app.getIsSubstitute()));
        }
      }
    } else {
      accountMap = new HashMap<>();
    }

    // 按分组ID分组安排
    Map<Long, List<WarArrangement>> arrangementsByGroup =
        arrangements.stream()
            .collect(
                Collectors.groupingBy(
                    arrangement ->
                        arrangement.getWarGroupId() != null ? arrangement.getWarGroupId() : -1L));

    // 处理机动人员（未分配到分组的人员）
    List<WarArrangement> mobileArrangements =
        arrangementsByGroup.getOrDefault(-1L, new ArrayList<>());
    List<GameAccountWithApplicationTime> mobileMembers =
        mobileArrangements.stream()
            .map(
                arrangement -> {
                  GameAccount account = accountMap.get(arrangement.getAccountId());
                  if (account != null) {
                    LocalDateTime appTime = applicationTimeMap.get(arrangement.getAccountId());
                    Boolean isSub = isSubstituteMap.get(arrangement.getAccountId());
                    return GameAccountWithApplicationTime.from(account, appTime, isSub);
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(
                GameAccountWithApplicationTime::getApplicationTime,
                Comparator.nullsFirst(Comparator.naturalOrder())
            ))
            .collect(Collectors.toList());
    response.setMobileMembers(mobileMembers);

    // 处理战事分组
    List<WarArrangementResponse.WarGroupDetail> warGroupDetails = new ArrayList<>();
    for (WarGroup warGroup : warGroups) {
      WarArrangementResponse.WarGroupDetail groupDetail =
          new WarArrangementResponse.WarGroupDetail();
      groupDetail.setGroupId(warGroup.getId());
      groupDetail.setGroupName(warGroup.getGroupName());
      groupDetail.setGroupTask(warGroup.getGroupTask());

      // 获取该分组的成员
      List<WarArrangement> groupArrangements =
          arrangementsByGroup.getOrDefault(warGroup.getId(), new ArrayList<>());
      List<GameAccountWithApplicationTime> groupMembers =
          groupArrangements.stream()
              .map(
                  arrangement -> {
                    GameAccount account = accountMap.get(arrangement.getAccountId());
                    if (account != null) {
                      LocalDateTime appTime = applicationTimeMap.get(arrangement.getAccountId());
                      Boolean isSub = isSubstituteMap.get(arrangement.getAccountId());
                      return GameAccountWithApplicationTime.from(account, appTime, isSub);
                    }
                    return null;
                  })
              .filter(Objects::nonNull)
              .sorted(Comparator.comparing(
                  GameAccountWithApplicationTime::getApplicationTime,
                  Comparator.nullsFirst(Comparator.naturalOrder())
              ))
              .collect(Collectors.toList());
      groupDetail.setMembers(groupMembers);

      warGroupDetails.add(groupDetail);
    }
    response.setWarGroups(warGroupDetails);

    return response;
  }

  public AccountWarArrangementResponse getAccountWarArrangements(Long accountId) {
    // 验证账号是否存在
    GameAccount account =
        gameAccountRepository
            .findById(accountId)
            .orElseThrow(() -> new BusinessException("游戏账号不存在"));

    AccountWarArrangementResponse response = new AccountWarArrangementResponse();
    response.setAccountId(accountId.toString());
    response.setAccount(account);

    // 获取该账号的所有战事安排
    List<WarArrangement> arrangements =
        warArrangementRepository.findByAccountIdOrderByCreatedAtAsc(accountId);

    if (arrangements.isEmpty()) {
      response.setWarArrangements(new ArrayList<>());
      return response;
    }

    // 按战事类型分组
    Map<WarType, List<WarArrangement>> arrangementsByWarType =
        arrangements.stream().collect(Collectors.groupingBy(WarArrangement::getWarType));

    List<AccountWarArrangementResponse.WarArrangementDetail> warArrangementDetails =
        new ArrayList<>();

    for (Map.Entry<WarType, List<WarArrangement>> entry : arrangementsByWarType.entrySet()) {
      WarType warType = entry.getKey();
      List<WarArrangement> warTypeArrangements = entry.getValue();

      // 每个战事类型应该只有一个安排记录
      WarArrangement arrangement = warTypeArrangements.get(0);

      AccountWarArrangementResponse.WarArrangementDetail detail =
          new AccountWarArrangementResponse.WarArrangementDetail();
      detail.setWarType(warType);
      detail.setIsSubstitute(Boolean.TRUE.equals(arrangement.getIsSubstitute()));

      if (arrangement.getWarGroupId() == null) {
        // 机动人员
        detail.setIsMobile(true);
        detail.setWarGroup(null);
      } else {
        // 分配到战事分组
        detail.setIsMobile(false);

        // 获取分组信息
        WarGroup warGroup = warGroupRepository.findById(arrangement.getWarGroupId()).orElse(null);

        if (warGroup != null) {
          AccountWarArrangementResponse.WarArrangementDetail.WarGroupInfo groupInfo =
              new AccountWarArrangementResponse.WarArrangementDetail.WarGroupInfo();
          groupInfo.setGroupId(warGroup.getId().toString());
          groupInfo.setGroupName(warGroup.getGroupName());
          groupInfo.setGroupTask(warGroup.getGroupTask());

          // 获取分组的所有成员
          List<WarArrangement> groupArrangements =
              warArrangementRepository.findByWarGroupIdOrderByCreatedAtAsc(warGroup.getId());

          List<Long> memberAccountIds =
              groupArrangements.stream()
                  .map(WarArrangement::getAccountId)
                  .collect(Collectors.toList());

          List<GameAccount> groupMembers = new ArrayList<>();
          if (!memberAccountIds.isEmpty()) {
            groupMembers = gameAccountRepository.findAllById(memberAccountIds);
          }
          groupInfo.setMembers(groupMembers);

          detail.setWarGroup(groupInfo);
        }
      }

      warArrangementDetails.add(detail);
    }

    response.setWarArrangements(warArrangementDetails);
    return response;
  }
}
