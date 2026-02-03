package com.app.gamehub.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.app.gamehub.dto.JoinAllianceRequest;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.AllianceApplication;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.User;
import com.app.gamehub.repository.*;
import com.app.gamehub.util.UserContext;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AllianceMemberServiceUnownedAccountTest {

  @InjectMocks
  private AllianceMemberService allianceMemberService;

  @Mock private AllianceRepository allianceRepository;

  @Mock private GameAccountRepository gameAccountRepository;

  @Mock private UserRepository userRepository;

  @Mock private AllianceApplicationRepository allianceApplicationRepository;

  @Mock private WarApplicationRepository warApplicationRepository;

  @Mock private WarArrangementRepository warArrangementRepository;

  @Mock private BarbarianGroupRepository barbarianGroupRepository;

  @Mock private PositionReservationRepository positionReservationRepository;

  @Mock private CarriageQueueRepository carriageQueueRepository;

  @Mock private EntityManager entityManager;

  @Mock private GameAccountService gameAccountService;

  private Alliance testAlliance;
  private User testUser;
  private GameAccount testAccount;

  @BeforeEach
  void setUp() {
    // 创建测试用户
    testUser = new User();
    testUser.setId(1L);
    testUser.setOpenid("test_openid");
    testUser.setNickname("测试用户");

    // 创建测试联盟
    testAlliance = new Alliance();
    testAlliance.setId(100L);
    testAlliance.setName("测试联盟");
    testAlliance.setCode("TEST01");
    testAlliance.setServerId(1);
    testAlliance.setLeaderId(testUser.getId());
    testAlliance.setAllianceJoinApprovalRequired(false);

    // 创建测试账号
    testAccount = new GameAccount();
    testAccount.setId(1L);
    testAccount.setUserId(testUser.getId());
    testAccount.setServerId(1);
    testAccount.setAccountName("测试账号");
    testAccount.setPowerValue(100L);
    testAccount.setAllianceId(testAlliance.getId()); // 将账号加入联盟
  }

  @Test
  void testBulkUpdateCreatesUnownedAccount() {
    // 准备测试数据 - 包含一个不存在的账号
    String testText =
        """
            1 测试账号 3 25 1500000 100 200 300
            2 不存在账号 2 20 800000 80 160 240
            """;

    // Mock behavior for bulk update
    when(allianceRepository.findById(testAlliance.getId())).thenReturn(Optional.of(testAlliance));
    when(gameAccountRepository.findByAllianceId(testAlliance.getId())).thenReturn(List.of(testAccount));
    when(gameAccountRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));

    try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
      mockedUserContext.when(UserContext::getUserId).thenReturn(testUser.getId());

      // 执行批量更新
      String result = allianceMemberService.bulkUpdateMembersFromText(testAlliance.getId(), testText);

      // 验证结果
      assertTrue(result.contains("已更新成员数: 1"));
      assertTrue(result.contains("已创建无主账号数: 1"));

      // 验证无主账号被创建
      verify(gameAccountRepository, atLeastOnce()).saveAll(argThat(accounts -> {
        List<GameAccount> accountList = (List<GameAccount>) accounts;
        return accountList.stream().anyMatch(account -> 
            account.getUserId() == null &&
            account.getAllianceId().equals(testAlliance.getId()) &&
            account.getAccountName().equals("不存在账号") &&
            account.getMemberTier() == GameAccount.MemberTier.TIER_2 &&
            account.getPowerValue().equals(80L)
        );
      }));
    }
  }

  @Test
  void testJoinAllianceWithUnownedAccount() {
    // 重置测试账号，使其不属于联盟
    testAccount.setAllianceId(null);

    // 先创建一个无主账号
    GameAccount unownedAccount = new GameAccount();
    unownedAccount.setId(2L);
    unownedAccount.setUserId(null); // 无主账号
    unownedAccount.setServerId(1);
    unownedAccount.setAccountName("测试账号"); // 与用户账号同名
    unownedAccount.setAllianceId(testAlliance.getId());
    unownedAccount.setMemberTier(GameAccount.MemberTier.TIER_3);
    unownedAccount.setPowerValue(200L);

    // Mock behavior
    when(allianceRepository.findByCode("TEST01")).thenReturn(Optional.of(testAlliance));
    when(gameAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
    when(gameAccountRepository.findByAllianceIdAndAccountName(testAlliance.getId(), "测试账号"))
        .thenReturn(Optional.of(unownedAccount));
    when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // 用户申请加入联盟
    JoinAllianceRequest request = new JoinAllianceRequest();
    request.setAccountId(testAccount.getId());
    request.setAllianceCode(testAlliance.getCode());

    try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
      mockedUserContext.when(UserContext::getUserId).thenReturn(testUser.getId());

      AllianceApplication result = allianceMemberService.applyToJoinAlliance(request);

      // 验证结果 - 应该返回null表示直接加入成功
      assertNull(result);

      // 验证无主账号被删除
      verify(gameAccountRepository).delete(unownedAccount);

      // 验证用户账号现在包含无主账号的信息
      verify(gameAccountRepository).save(argThat(account -> 
          account.getId().equals(testAccount.getId()) &&
          account.getUserId().equals(testUser.getId()) &&
          account.getAllianceId().equals(testAlliance.getId()) &&
          account.getMemberTier() == GameAccount.MemberTier.TIER_3 &&
          account.getPowerValue().equals(200L)
      ));
    }
  }

  @Test
  void testNormalJoinAllianceWhenNoUnownedAccount() {
    // 重置测试账号，使其不属于联盟
    testAccount.setAllianceId(null);

    // Mock behavior - 没有同名无主账号
    when(allianceRepository.findByCode("TEST01")).thenReturn(Optional.of(testAlliance));
    when(gameAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
    when(gameAccountRepository.findByAllianceIdAndAccountName(testAlliance.getId(), "测试账号"))
        .thenReturn(Optional.empty());
    when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(allianceApplicationRepository.save(any(AllianceApplication.class))).thenAnswer(invocation -> {
      AllianceApplication app = invocation.getArgument(0);
      app.setId(1L);
      return app;
    });

    // 用户申请加入联盟（没有同名无主账号）
    JoinAllianceRequest request = new JoinAllianceRequest();
    request.setAccountId(testAccount.getId());
    request.setAllianceCode(testAlliance.getCode());

    try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
      mockedUserContext.when(UserContext::getUserId).thenReturn(testUser.getId());

      AllianceApplication result = allianceMemberService.applyToJoinAlliance(request);

      // 验证结果 - 由于联盟不需要审核，应该返回已批准的申请
      assertNotNull(result);
      assertEquals(AllianceApplication.ApplicationStatus.APPROVED, result.getStatus());

      // 验证账号加入了联盟
      verify(gameAccountRepository).save(argThat(account -> 
          account.getId().equals(testAccount.getId()) &&
          account.getAllianceId().equals(testAlliance.getId()) &&
          account.getMemberTier() == GameAccount.MemberTier.TIER_1
      ));
    }
  }
}
