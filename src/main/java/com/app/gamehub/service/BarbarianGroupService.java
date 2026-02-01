package com.app.gamehub.service;

import com.app.gamehub.dto.BarbarianGroupDetailResponse;
import com.app.gamehub.dto.BarbarianGroupResponse;
import com.app.gamehub.dto.CreateAndJoinBarbarianGroupRequest;
import com.app.gamehub.dto.CreateBarbarianGroupRequest;
import com.app.gamehub.dto.JoinBarbarianGroupRequest;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.BarbarianGroup;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.BarbarianGroupRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.util.UserContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarbarianGroupService {

  private final BarbarianGroupRepository barbarianGroupRepository;
  private final GameAccountRepository gameAccountRepository;
  private final AllianceRepository allianceRepository;

  /**
   * 创建南蛮分组
   */
  @Transactional
  public BarbarianGroup createBarbarianGroup(CreateBarbarianGroupRequest request) {
    Long userId = UserContext.getUserId();

    // 验证联盟存在
    Alliance alliance = allianceRepository.findById(request.getAllianceId())
        .orElseThrow(() -> new BusinessException("联盟不存在"));

    // 创建分组
    BarbarianGroup group = new BarbarianGroup();
    group.setAllianceId(request.getAllianceId());
    group.setGroupName(request.getGroupName());
    group.setQueueCount(request.getQueueCount());

    BarbarianGroup savedGroup = barbarianGroupRepository.save(group);

    log.info("用户 {} 创建了南蛮分组 {}", userId, savedGroup.getId());
    return savedGroup;
  }

  /**
   * 创建南蛮分组并加入
   */
  @Transactional
  public GameAccount createAndJoinBarbarianGroup(CreateAndJoinBarbarianGroupRequest request) {
    Long userId = UserContext.getUserId();

    // 验证联盟存在
    Alliance alliance = allianceRepository.findById(request.getAllianceId())
        .orElseThrow(() -> new BusinessException("联盟不存在"));

    // 验证账号存在且属于当前用户
    GameAccount account = gameAccountRepository.findById(request.getAccountId())
        .orElseThrow(() -> new BusinessException("账号不存在"));

    if (account.getUserId() == null || !account.getUserId().equals(userId)) {
      throw new BusinessException("只能操作自己的账号");
    }

    // 验证账号是否属于该联盟
    if (!account.getAllianceId().equals(request.getAllianceId())) {
      throw new BusinessException("只能在自己所在的联盟中创建分组");
    }

    // 创建分组
    BarbarianGroup group = new BarbarianGroup();
    group.setAllianceId(request.getAllianceId());
    group.setGroupName(request.getGroupName());
    group.setQueueCount(request.getQueueCount());

    BarbarianGroup savedGroup = barbarianGroupRepository.save(group);

    // 账号加入分组
    GameAccount updatedAccount = joinGroupInternal(account, savedGroup);

    log.info("用户 {} 的账号 {} 创建并加入了南蛮分组 {}", userId, request.getAccountId(), savedGroup.getId());
    return updatedAccount;
  }

  /**
   * 加入南蛮分组
   */
  @Transactional
  public GameAccount joinBarbarianGroup(JoinBarbarianGroupRequest request) {
    Long userId = UserContext.getUserId();

    // 验证分组存在
    BarbarianGroup group = barbarianGroupRepository.findById(request.getGroupId())
        .orElseThrow(() -> new BusinessException("南蛮分组不存在"));

    // 验证账号存在且属于当前用户
    GameAccount account = gameAccountRepository.findById(request.getAccountId())
        .orElseThrow(() -> new BusinessException("账号不存在"));

    if (account.getUserId() == null || !account.getUserId().equals(userId)) {
      throw new BusinessException("只能操作自己的账号");
    }

    // 验证账号是否属于该联盟
    if (!account.getAllianceId().equals(group.getAllianceId())) {
      throw new BusinessException("只能加入自己所在联盟的分组");
    }

    return joinGroupInternal(account, group);
  }

  /**
   * 内部方法：加入分组的具体逻辑
   */
  private GameAccount joinGroupInternal(GameAccount account, BarbarianGroup group) {
    // 如果账号已经在其他分组中，需要先处理原分组
    if (account.getBarbarianGroupId() != null) {
      Long oldGroupId = account.getBarbarianGroupId();
      
      // 先离开原分组
      account.setBarbarianGroupId(null);
      gameAccountRepository.save(account);
      
      // 检查原分组是否还有其他成员，如果没有则删除
      long memberCount = barbarianGroupRepository.countMembersByGroupId(oldGroupId);
      if (memberCount == 0) {
        barbarianGroupRepository.deleteById(oldGroupId);
        log.info("南蛮分组 {} 因无成员而被自动删除", oldGroupId);
      }
    }

    // 加入新分组
    account.setBarbarianGroupId(group.getId());
    GameAccount savedAccount = gameAccountRepository.save(account);

    log.info("账号 {} 加入了南蛮分组 {}", account.getId(), group.getId());
    return savedAccount;
  }

  /**
   * 离开南蛮分组
   */
  @Transactional
  public GameAccount leaveBarbarianGroup(Long accountId) {
    Long userId = UserContext.getUserId();

    // 验证账号存在且属于当前用户
    GameAccount account = gameAccountRepository.findById(accountId)
        .orElseThrow(() -> new BusinessException("账号不存在"));

    if (account.getUserId() == null || !account.getUserId().equals(userId)) {
      throw new BusinessException("只能操作自己的账号");
    }

    if (account.getBarbarianGroupId() == null) {
      throw new BusinessException("账号未加入任何南蛮分组");
    }

    Long groupId = account.getBarbarianGroupId();
    
    // 离开分组
    account.setBarbarianGroupId(null);
    GameAccount savedAccount = gameAccountRepository.save(account);

    // 检查分组是否还有其他成员，如果没有则删除
    long memberCount = barbarianGroupRepository.countMembersByGroupId(groupId);
    if (memberCount == 0) {
      barbarianGroupRepository.deleteById(groupId);
      log.info("南蛮分组 {} 因无成员而被自动删除", groupId);
    }

    log.info("账号 {} 离开了南蛮分组 {}", accountId, groupId);
    return savedAccount;
  }

  /**
   * 获取联盟的所有南蛮分组
   */
  public List<BarbarianGroupResponse> getAllianceBarbarianGroups(Long allianceId) {
    List<BarbarianGroup> groups = barbarianGroupRepository.findByAllianceIdOrderByCreatedAtDesc(allianceId);
    return groups.stream().map(this::convertToResponse).toList();
  }

  /**
   * 根据队列数量查询联盟中的南蛮分组
   */
  public List<BarbarianGroupResponse> getBarbarianGroupsByQueueCount(Long allianceId, Integer queueCount) {
    List<BarbarianGroup> groups = barbarianGroupRepository.findByAllianceIdAndQueueCountOrderByCreatedAtDesc(allianceId, queueCount);
    return groups.stream().map(this::convertToResponse).toList();
  }

  /**
   * 获取分组详情（包含成员信息）
   */
  public BarbarianGroupDetailResponse getBarbarianGroupDetail(Long groupId) {
    BarbarianGroup group = barbarianGroupRepository.findById(groupId)
        .orElseThrow(() -> new BusinessException("南蛮分组不存在"));

    List<GameAccount> members = gameAccountRepository.findByBarbarianGroupIdOrderByPowerValueDesc(groupId);

    BarbarianGroupDetailResponse response = new BarbarianGroupDetailResponse();
    response.setGroup(convertToResponse(group));
    response.setMembers(members);
    response.setMemberCount(members.size());

    return response;
  }

  /**
   * 获取账号所在的南蛮分组
   */
  public BarbarianGroupResponse getAccountBarbarianGroup(Long accountId) {
    GameAccount account = gameAccountRepository.findById(accountId)
        .orElseThrow(() -> new BusinessException("账号不存在"));

    if (account.getBarbarianGroupId() == null) {
      return null;
    }

    BarbarianGroup group = barbarianGroupRepository.findById(account.getBarbarianGroupId())
        .orElse(null);

    return group != null ? convertToResponse(group) : null;
  }

  /**
   * 将BarbarianGroup实体转换为BarbarianGroupResponse DTO
   */
  private BarbarianGroupResponse convertToResponse(BarbarianGroup group) {
    BarbarianGroupResponse response = new BarbarianGroupResponse();
    response.setId(group.getId().toString());
    response.setAllianceId(group.getAllianceId().toString());
    response.setGroupName(group.getGroupName());
    response.setQueueCount(group.getQueueCount());
    response.setCreatedAt(group.getCreatedAt());
    response.setUpdatedAt(group.getUpdatedAt());

    return response;
  }
}
