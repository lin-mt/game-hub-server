package com.app.gamehub.service;

import com.app.gamehub.dto.AllianceDetailResponse;
import com.app.gamehub.entity.*;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.*;
import com.app.gamehub.util.UserContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueryService {

  private final AllianceRepository allianceRepository;

  private final GameAccountRepository gameAccountRepository;

  private final WarGroupRepository warGroupRepository;

  private final WarArrangementRepository warArrangementRepository;

  private final AllianceApplicationRepository allianceApplicationRepository;

  private final WarApplicationRepository warApplicationRepository;

  public QueryService(
      AllianceRepository allianceRepository,
      GameAccountRepository gameAccountRepository,
      WarGroupRepository warGroupRepository,
      WarArrangementRepository warArrangementRepository,
      AllianceApplicationRepository allianceApplicationRepository,
      WarApplicationRepository warApplicationRepository) {
    this.allianceRepository = allianceRepository;
    this.gameAccountRepository = gameAccountRepository;
    this.warGroupRepository = warGroupRepository;
    this.warArrangementRepository = warArrangementRepository;
    this.allianceApplicationRepository = allianceApplicationRepository;
    this.warApplicationRepository = warApplicationRepository;
  }

  public AllianceDetailResponse getAllianceDetail(Long allianceId) {
    // 获取联盟基本信息
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    // 获取联盟成员
    List<GameAccount> members =
        gameAccountRepository.findByAllianceIdOrderByPowerValueDesc(allianceId);

    // 获取所有战事分组
    List<WarGroup> allWarGroups =
        warGroupRepository.findByAllianceIdOrderByWarTypeAscCreatedAtAsc(allianceId);
    Map<String, List<WarGroup>> warGroupsByType =
        allWarGroups.stream().collect(Collectors.groupingBy(group -> group.getWarType().name()));

    // 获取战事人员安排
    List<WarArrangement> warArrangements =
        warArrangementRepository.findByAllianceIdOrderByCreatedAtAsc(
            allianceId);

    AllianceDetailResponse response = new AllianceDetailResponse();
    response.setAlliance(alliance);
    response.setMembers(members);
    response.setWarGroups(warGroupsByType);
    response.setWarArrangements(warArrangements);

    return response;
  }

  public List<AllianceApplication> getAccountAllianceApplicationStatus(Long accountId) {
    return allianceApplicationRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
  }

  public List<WarApplication> getAccountWarApplicationStatus(Long accountId) {
    return warApplicationRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
  }

  public Map<String, List<WarApplication>> getAllianceWarApplications(Long allianceId) {
    Long userId = UserContext.getUserId();
    // 验证是否为盟主
    Alliance alliance =
        allianceRepository.findById(allianceId).orElseThrow(() -> new BusinessException("联盟不存在"));

    if (!alliance.getLeaderId().equals(userId)) {
      throw new BusinessException("只有盟主可以查看申请列表");
    }

    Map<String, List<WarApplication>> result = new HashMap<>();

    // 获取官渡一申请
    List<WarApplication> guanduOneApps =
        warApplicationRepository.findByAllianceIdAndWarTypeAndStatusOrderByCreatedAtAsc(
            allianceId, WarType.GUANDU_ONE, WarApplication.ApplicationStatus.PENDING);
    result.put("GUANDU_ONE", guanduOneApps);

    // 获取官渡二申请
    List<WarApplication> guanduTwoApps =
        warApplicationRepository.findByAllianceIdAndWarTypeAndStatusOrderByCreatedAtAsc(
            allianceId, WarType.GUANDU_TWO, WarApplication.ApplicationStatus.PENDING);
    result.put("GUANDU_TWO", guanduTwoApps);

    return result;
  }
}
