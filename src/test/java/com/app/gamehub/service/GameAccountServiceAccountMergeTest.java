package com.app.gamehub.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.app.gamehub.dto.UpdateGameAccountRequest;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.User;
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
public class GameAccountServiceAccountMergeTest {

    @InjectMocks
    private GameAccountService gameAccountService;

    @Mock
    private GameAccountRepository gameAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AllianceRepository allianceRepository;

    @Mock
    private AllianceApplicationRepository allianceApplicationRepository;

    @Mock
    private WarApplicationRepository warApplicationRepository;

    @Mock
    private WarArrangementRepository warArrangementRepository;

    @Mock
    private PositionReservationRepository positionReservationRepository;

    @Mock
    private CarriageQueueRepository carriageQueueRepository;

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

        // 创建用户账号（属于联盟）
        testAccount = new GameAccount();
        testAccount.setId(1L);
        testAccount.setUserId(testUser.getId());
        testAccount.setServerId(1);
        testAccount.setAccountName("原账号名");
        testAccount.setAllianceId(testAlliance.getId());
        testAccount.setPowerValue(100L);
        testAccount.setTroopLevel(30);

        // 创建无主账号（属于联盟，有更多信息）
        unownedAccount = new GameAccount();
        unownedAccount.setId(2L);
        unownedAccount.setUserId(null);
        unownedAccount.setServerId(1);
        unownedAccount.setAccountName("新账号名"); // 用户要修改成的名称
        unownedAccount.setAllianceId(testAlliance.getId());
        unownedAccount.setMemberTier(GameAccount.MemberTier.TIER_3);
        unownedAccount.setPowerValue(200L);
        unownedAccount.setTroopLevel(50);
        unownedAccount.setRallyCapacity(100000);
        unownedAccount.setInfantryDefense(1500);
    }

    @Test
    void testUpdateAccountNameWithUnownedAccountMerge() {
        // Mock repository behavior
        when(gameAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(gameAccountRepository.findByAllianceIdAndAccountName(testAlliance.getId(), "新账号名"))
            .thenReturn(Optional.of(unownedAccount));
        when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 创建更新请求
        UpdateGameAccountRequest request = new UpdateGameAccountRequest();
        request.setAccountName("新账号名");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(testUser.getId());

            // 执行更新
            GameAccount result = gameAccountService.updateGameAccount(testAccount.getId(), request);

            // 验证结果
            assertNotNull(result);
            assertEquals("新账号名", result.getAccountName());
            assertEquals(testUser.getId(), result.getUserId());
            assertEquals(testAlliance.getId(), result.getAllianceId());

            // 验证无主账号的信息被合并
            assertEquals(GameAccount.MemberTier.TIER_3, result.getMemberTier());
            assertEquals(200L, result.getPowerValue());
            assertEquals(50, result.getTroopLevel());
            assertEquals(100000, result.getRallyCapacity());
            assertEquals(1500, result.getInfantryDefense());

            // 验证无主账号被删除
            verify(gameAccountRepository).delete(unownedAccount);

            // 验证关联记录的转移
            verify(allianceApplicationRepository).transferToAccount(unownedAccount.getId(), testAccount.getId());
            verify(warApplicationRepository).transferToAccount(unownedAccount.getId(), testAccount.getId());
            verify(warArrangementRepository).transferToAccount(unownedAccount.getId(), testAccount.getId());
            verify(positionReservationRepository).transferToAccount(unownedAccount.getId(), testAccount.getId());
            verify(carriageQueueRepository).transferToAccount(unownedAccount.getId(), testAccount.getId());

            // 验证EntityManager操作
            verify(entityManager, times(6)).flush(); // 5次转移记录 + 1次删除无主账号
            verify(entityManager).clear();
        }
    }

    @Test
    void testUpdateAccountNameWithoutUnownedAccount() {
        // Mock repository behavior - 没有同名无主账号
        when(gameAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(gameAccountRepository.findByAllianceIdAndAccountName(testAlliance.getId(), "新账号名"))
            .thenReturn(Optional.empty());
        when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 创建更新请求
        UpdateGameAccountRequest request = new UpdateGameAccountRequest();
        request.setAccountName("新账号名");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(testUser.getId());

            // 执行更新
            GameAccount result = gameAccountService.updateGameAccount(testAccount.getId(), request);

            // 验证结果
            assertNotNull(result);
            assertEquals("新账号名", result.getAccountName());
            assertEquals(testUser.getId(), result.getUserId());

            // 验证原有信息保持不变
            assertEquals(100L, result.getPowerValue());
            assertEquals(30, result.getTroopLevel());

            // 验证没有删除操作
            verify(gameAccountRepository, never()).delete(any());

            // 验证没有转移操作
            verify(allianceApplicationRepository, never()).transferToAccount(any(), any());
            verify(warApplicationRepository, never()).transferToAccount(any(), any());
            verify(warArrangementRepository, never()).transferToAccount(any(), any());
            verify(positionReservationRepository, never()).transferToAccount(any(), any());
            verify(carriageQueueRepository, never()).transferToAccount(any(), any());
        }
    }

    @Test
    void testUpdateAccountNameWithOwnedAccount() {
        // 创建一个有主的同名账号
        GameAccount ownedAccount = new GameAccount();
        ownedAccount.setId(3L);
        ownedAccount.setUserId(2L); // 不同的用户ID
        ownedAccount.setServerId(1);
        ownedAccount.setAccountName("新账号名");
        ownedAccount.setAllianceId(testAlliance.getId());

        // Mock repository behavior - 找到的是有主账号
        when(gameAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(gameAccountRepository.findByAllianceIdAndAccountName(testAlliance.getId(), "新账号名"))
            .thenReturn(Optional.of(ownedAccount));
        when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 创建更新请求
        UpdateGameAccountRequest request = new UpdateGameAccountRequest();
        request.setAccountName("新账号名");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(testUser.getId());

            // 执行更新
            GameAccount result = gameAccountService.updateGameAccount(testAccount.getId(), request);

            // 验证结果 - 正常更新，不进行合并
            assertNotNull(result);
            assertEquals("新账号名", result.getAccountName());
            assertEquals(testUser.getId(), result.getUserId());

            // 验证原有信息保持不变
            assertEquals(100L, result.getPowerValue());
            assertEquals(30, result.getTroopLevel());

            // 验证没有删除操作
            verify(gameAccountRepository, never()).delete(any());

            // 验证没有转移操作
            verify(allianceApplicationRepository, never()).transferToAccount(any(), any());
        }
    }

    @Test
    void testUpdateAccountNameNotInAlliance() {
        // 用户账号不属于联盟
        testAccount.setAllianceId(null);

        // Mock repository behavior
        when(gameAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 创建更新请求
        UpdateGameAccountRequest request = new UpdateGameAccountRequest();
        request.setAccountName("新账号名");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(testUser.getId());

            // 执行更新
            GameAccount result = gameAccountService.updateGameAccount(testAccount.getId(), request);

            // 验证结果 - 正常更新，不进行合并
            assertNotNull(result);
            assertEquals("新账号名", result.getAccountName());
            assertEquals(testUser.getId(), result.getUserId());

            // 验证没有查找同名账号
            verify(gameAccountRepository, never()).findByAllianceIdAndAccountName(any(), any());

            // 验证没有删除操作
            verify(gameAccountRepository, never()).delete(any());
        }
    }

    @Test
    void testUpdateAccountNameWithPartialMerge() {
        // 无主账号只有部分字段有值
        unownedAccount.setPowerValue(null); // 战力为空
        unownedAccount.setTroopLevel(null); // 部队等级为空
        unownedAccount.setRallyCapacity(50000); // 只有集结容量有值
        unownedAccount.setMemberTier(GameAccount.MemberTier.TIER_2);

        // Mock repository behavior
        when(gameAccountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(gameAccountRepository.findByAllianceIdAndAccountName(testAlliance.getId(), "新账号名"))
            .thenReturn(Optional.of(unownedAccount));
        when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 创建更新请求
        UpdateGameAccountRequest request = new UpdateGameAccountRequest();
        request.setAccountName("新账号名");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(testUser.getId());

            // 执行更新
            GameAccount result = gameAccountService.updateGameAccount(testAccount.getId(), request);

            // 验证结果
            assertNotNull(result);
            assertEquals("新账号名", result.getAccountName());

            // 验证合并结果
            // 原有数据应该保持不变（因为无主账号对应字段为null）
            assertEquals(100L, result.getPowerValue());
            assertEquals(30, result.getTroopLevel());
            // 无主账号的非空字段应该被复制
            assertEquals(50000, result.getRallyCapacity());
            assertEquals(GameAccount.MemberTier.TIER_2, result.getMemberTier());

            // 验证无主账号被删除
            verify(gameAccountRepository).delete(unownedAccount);
        }
    }
}