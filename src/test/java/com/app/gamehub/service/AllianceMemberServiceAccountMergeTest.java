package com.app.gamehub.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.app.gamehub.dto.JoinAllianceRequest;
import com.app.gamehub.entity.*;
import com.app.gamehub.repository.*;
import com.app.gamehub.util.UserContext;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AllianceMemberServiceAccountMergeTest {

    @InjectMocks
    private AllianceMemberService allianceMemberService;

    @Mock
    private AllianceRepository allianceRepository;

    @Mock
    private GameAccountRepository gameAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WarApplicationRepository warApplicationRepository;

    @Mock
    private WarArrangementRepository warArrangementRepository;

    @Mock
    private PositionReservationRepository positionReservationRepository;

    @Mock
    private CarriageQueueRepository carriageQueueRepository;

    @Mock
    private AllianceApplicationRepository allianceApplicationRepository;

    @Mock
    private BarbarianGroupRepository barbarianGroupRepository;

    @Mock
    private EntityManager entityManager;

    private Alliance testAlliance;
    private User testUser;
    private GameAccount testAccount;
    private GameAccount unownedAccount;

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

        // 创建用户账号（不属于联盟）
        testAccount = new GameAccount();
        testAccount.setId(1L);
        testAccount.setUserId(testUser.getId());
        testAccount.setServerId(1);
        testAccount.setAccountName("测试账号");
        testAccount.setPowerValue(100L);

        // 创建无主账号（属于联盟，有更多信息）
        unownedAccount = new GameAccount();
        unownedAccount.setId(2L);
        unownedAccount.setUserId(null);
        unownedAccount.setServerId(1);
        unownedAccount.setAccountName("测试账号"); // 与用户账号同名
        unownedAccount.setAllianceId(testAlliance.getId());
        unownedAccount.setMemberTier(GameAccount.MemberTier.TIER_3);
        unownedAccount.setPowerValue(200L);
        unownedAccount.setTroopLevel(50);
        unownedAccount.setRallyCapacity(100000);
    }

    @Test
    void testAccountMergeWithAssociatedRecords() {
        // Mock repository behavior for this specific test
        when(allianceRepository.findByCode("TEST01")).thenReturn(Optional.of(testAlliance));
        when(gameAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(gameAccountRepository.findByAllianceIdAndAccountName(testAlliance.getId(), "测试账号"))
            .thenReturn(Optional.of(unownedAccount));
        when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行加入联盟操作
        JoinAllianceRequest request = new JoinAllianceRequest();
        request.setAccountId(testAccount.getId());
        request.setAllianceCode(testAlliance.getCode());

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(testUser.getId());

            System.out.println("Before merge - testAccount.getId(): " + testAccount.getId());
            System.out.println("Before merge - unownedAccount.getId(): " + unownedAccount.getId());

            AllianceApplication result = allianceMemberService.applyToJoinAlliance(request);

            System.out.println("After merge - testAccount.getId(): " + testAccount.getId());

            // 验证结果 - 应该返回null表示直接加入成功
            assertNull(result);

            // 验证账号合并结果
            verify(gameAccountRepository).save(argThat(account -> 
                account.getId().equals(testAccount.getId()) &&
                account.getUserId().equals(testUser.getId()) &&
                account.getAllianceId().equals(testAlliance.getId()) &&
                account.getMemberTier() == GameAccount.MemberTier.TIER_3 &&
                account.getPowerValue().equals(200L) &&
                account.getTroopLevel().equals(50) &&
                account.getRallyCapacity().equals(100000)
            ));

            // 验证无主账号被删除
            verify(gameAccountRepository).delete(unownedAccount);

            // 验证关联记录的转移
            verify(allianceApplicationRepository).transferToAccount(unownedAccount.getId(), testAccount.getId());
            verify(warApplicationRepository).transferToAccount(unownedAccount.getId(), testAccount.getId());
            verify(warArrangementRepository).transferToAccount(unownedAccount.getId(), testAccount.getId());
            verify(positionReservationRepository).transferToAccount(unownedAccount.getId(), testAccount.getId());
            verify(carriageQueueRepository).transferToAccount(unownedAccount.getId(), testAccount.getId());
        }
    }

    @Test
    void testAccountMergeWithPartialData() {
        // 测试只有部分字段有值的情况
        unownedAccount.setPowerValue(null); // 战力为空
        unownedAccount.setTroopLevel(null); // 部队等级为空
        unownedAccount.setRallyCapacity(50000); // 只有集结容量有值

        // 用户账号有一些原有数据
        testAccount.setPowerValue(150L);
        testAccount.setTroopLevel(30);

        // Mock repository behavior for this specific test
        when(allianceRepository.findByCode("TEST01")).thenReturn(Optional.of(testAlliance));
        when(gameAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(gameAccountRepository.findByAllianceIdAndAccountName(testAlliance.getId(), "测试账号"))
            .thenReturn(Optional.of(unownedAccount));
        when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行合并
        JoinAllianceRequest request = new JoinAllianceRequest();
        request.setAccountId(testAccount.getId());
        request.setAllianceCode(testAlliance.getCode());

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(testUser.getId());

            AllianceApplication result = allianceMemberService.applyToJoinAlliance(request);

            // 验证结果 - 应该返回null表示直接加入成功
            assertNull(result);

            // 验证合并结果
            verify(gameAccountRepository).save(argThat(account -> 
                account.getId().equals(testAccount.getId()) &&
                // 原有数据应该保持不变（因为无主账号对应字段为null）
                account.getPowerValue().equals(150L) &&
                account.getTroopLevel().equals(30) &&
                // 无主账号的非空字段应该被复制
                account.getRallyCapacity().equals(50000) &&
                account.getMemberTier() == GameAccount.MemberTier.TIER_3
            ));
        }
    }
}