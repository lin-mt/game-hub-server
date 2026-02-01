package com.app.gamehub.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.app.gamehub.dto.CreateGameAccountRequest;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.*;
import com.app.gamehub.util.UserContext;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameAccountServiceAllianceCreationTest {

  @Mock private GameAccountRepository gameAccountRepository;
  @Mock private UserRepository userRepository;
  @Mock private AllianceRepository allianceRepository;
  @Mock private AllianceApplicationRepository allianceApplicationRepository;
  @Mock private WarApplicationRepository warApplicationRepository;
  @Mock private WarArrangementRepository warArrangementRepository;
  @Mock private PositionReservationRepository positionReservationRepository;
  @Mock private BarbarianGroupRepository barbarianGroupRepository;
  @Mock private CarriageQueueRepository carriageQueueRepository;
  @Mock private EntityManager entityManager;

  @InjectMocks private GameAccountService gameAccountService;

  private CreateGameAccountRequest request;
  private Alliance alliance;
  private GameAccount unownedAccount;

  @BeforeEach
  void setUp() {
    request = new CreateGameAccountRequest();
    request.setServerId(1);
    request.setAccountName("TestAccount");
    request.setDamageBonus(new BigDecimal("10.5"));
    request.setTroopLevel(5);
    request.setAllianceId(100L);

    alliance = new Alliance();
    alliance.setId(100L);
    alliance.setServerId(1);
    alliance.setName("TestAlliance");

    unownedAccount = new GameAccount();
    unownedAccount.setId(200L);
    unownedAccount.setUserId(null); // 无主账号
    unownedAccount.setServerId(1);
    unownedAccount.setAccountName("TestAccount");
    unownedAccount.setAllianceId(100L);
    unownedAccount.setPowerValue(50L);
    unownedAccount.setDamageBonus(new BigDecimal("15.0"));
    unownedAccount.setMemberTier(GameAccount.MemberTier.TIER_3);
  }

  @Test
  void testCreateAccountWithAllianceId_Success() {
    try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
      // Arrange
      Long userId = 1L;
      userContextMock.when(UserContext::getUserId).thenReturn(userId);

      when(userRepository.existsById(userId)).thenReturn(true);
      when(gameAccountRepository.countByUserIdAndServerId(userId, 1)).thenReturn(0L);
      when(gameAccountRepository.findByAccountNameAndServerIdAndUserId("TestAccount", 1, userId))
          .thenReturn(Optional.empty());
      when(allianceRepository.findById(100L)).thenReturn(Optional.of(alliance));
      when(gameAccountRepository.findByAllianceIdAndAccountName(100L, "TestAccount"))
          .thenReturn(Optional.empty());

      GameAccount savedAccount = new GameAccount();
      savedAccount.setId(1L);
      savedAccount.setUserId(userId);
      savedAccount.setAllianceId(100L);
      when(gameAccountRepository.save(any(GameAccount.class))).thenReturn(savedAccount);

      // Act
      GameAccount result = gameAccountService.createGameAccount(request);

      // Assert
      assertNotNull(result);
      assertEquals(100L, result.getAllianceId());
      verify(gameAccountRepository).save(argThat(account -> 
          account.getAllianceId().equals(100L) && 
          account.getMemberTier() == GameAccount.MemberTier.TIER_1
      ));
    }
  }

  @Test
  void testCreateAccountWithAllianceId_MergeUnownedAccount() {
    try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
      // Arrange
      Long userId = 1L;
      userContextMock.when(UserContext::getUserId).thenReturn(userId);

      when(userRepository.existsById(userId)).thenReturn(true);
      when(gameAccountRepository.countByUserIdAndServerId(userId, 1)).thenReturn(0L);
      when(gameAccountRepository.findByAccountNameAndServerIdAndUserId("TestAccount", 1, userId))
          .thenReturn(Optional.empty());
      when(allianceRepository.findById(100L)).thenReturn(Optional.of(alliance));
      when(gameAccountRepository.findByAllianceIdAndAccountName(100L, "TestAccount"))
          .thenReturn(Optional.of(unownedAccount));

      GameAccount savedAccount = new GameAccount();
      savedAccount.setId(1L);
      savedAccount.setUserId(userId);
      savedAccount.setAllianceId(100L);
      savedAccount.setPowerValue(50L); // 从无主账号合并的战力
      savedAccount.setDamageBonus(new BigDecimal("15.0")); // 从无主账号合并的加成
      savedAccount.setMemberTier(GameAccount.MemberTier.TIER_3); // 从无主账号合并的阶级
      when(gameAccountRepository.save(any(GameAccount.class))).thenReturn(savedAccount);

      // Act
      GameAccount result = gameAccountService.createGameAccount(request);

      // Assert
      assertNotNull(result);
      assertEquals(100L, result.getAllianceId());
      assertEquals(50L, result.getPowerValue()); // 验证合并了无主账号的战力
      assertEquals(new BigDecimal("15.0"), result.getDamageBonus()); // 验证合并了无主账号的加成
      assertEquals(GameAccount.MemberTier.TIER_3, result.getMemberTier()); // 验证合并了无主账号的阶级

      // 验证转移关联记录的调用
      verify(allianceApplicationRepository).transferToAccount(200L, 1L);
      verify(warApplicationRepository).transferToAccount(200L, 1L);
      verify(warArrangementRepository).transferToAccount(200L, 1L);
      verify(positionReservationRepository).transferToAccount(200L, 1L);
      verify(carriageQueueRepository).transferToAccount(200L, 1L);

      // 验证删除无主账号
      verify(gameAccountRepository).delete(unownedAccount);
      verify(entityManager, times(6)).flush(); // 5次转移 + 1次删除
      verify(entityManager).clear();
    }
  }

  @Test
  void testCreateAccountWithAllianceId_AllianceNotFound() {
    try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
      // Arrange
      Long userId = 1L;
      userContextMock.when(UserContext::getUserId).thenReturn(userId);

      when(userRepository.existsById(userId)).thenReturn(true);
      when(gameAccountRepository.countByUserIdAndServerId(userId, 1)).thenReturn(0L);
      when(gameAccountRepository.findByAccountNameAndServerIdAndUserId("TestAccount", 1, userId))
          .thenReturn(Optional.empty());
      when(allianceRepository.findById(100L)).thenReturn(Optional.empty());

      // Act & Assert
      BusinessException exception = assertThrows(BusinessException.class, () -> {
        gameAccountService.createGameAccount(request);
      });
      assertEquals("联盟不存在", exception.getMessage());
    }
  }

  @Test
  void testCreateAccountWithAllianceId_DifferentServer() {
    try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
      // Arrange
      Long userId = 1L;
      userContextMock.when(UserContext::getUserId).thenReturn(userId);

      alliance.setServerId(2); // 不同的区号

      when(userRepository.existsById(userId)).thenReturn(true);
      when(gameAccountRepository.countByUserIdAndServerId(userId, 1)).thenReturn(0L);
      when(gameAccountRepository.findByAccountNameAndServerIdAndUserId("TestAccount", 1, userId))
          .thenReturn(Optional.empty());
      when(allianceRepository.findById(100L)).thenReturn(Optional.of(alliance));

      // Act & Assert
      BusinessException exception = assertThrows(BusinessException.class, () -> {
        gameAccountService.createGameAccount(request);
      });
      assertEquals("只能加入同一个区的联盟", exception.getMessage());
    }
  }

  @Test
  void testCreateAccountWithoutAllianceId_Success() {
    try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
      // Arrange
      Long userId = 1L;
      userContextMock.when(UserContext::getUserId).thenReturn(userId);
      request.setAllianceId(null); // 不提供联盟ID

      when(userRepository.existsById(userId)).thenReturn(true);
      when(gameAccountRepository.countByUserIdAndServerId(userId, 1)).thenReturn(0L);
      when(gameAccountRepository.findByAccountNameAndServerIdAndUserId("TestAccount", 1, userId))
          .thenReturn(Optional.empty());

      GameAccount savedAccount = new GameAccount();
      savedAccount.setId(1L);
      savedAccount.setUserId(userId);
      savedAccount.setAllianceId(null); // 没有联盟
      when(gameAccountRepository.save(any(GameAccount.class))).thenReturn(savedAccount);

      // Act
      GameAccount result = gameAccountService.createGameAccount(request);

      // Assert
      assertNotNull(result);
      assertNull(result.getAllianceId()); // 验证没有加入联盟
      verify(gameAccountRepository).save(argThat(account -> 
          account.getAllianceId() == null
      ));
    }
  }

  @Test
  void testCreateAccount_ExceedAccountLimit() {
    try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
      // Arrange
      Long userId = 1L;
      userContextMock.when(UserContext::getUserId).thenReturn(userId);

      when(userRepository.existsById(userId)).thenReturn(true);
      when(gameAccountRepository.countByUserIdAndServerId(userId, 1)).thenReturn(2L); // 已有2个账号

      // Act & Assert
      BusinessException exception = assertThrows(BusinessException.class, () -> {
        gameAccountService.createGameAccount(request);
      });
      assertEquals("每个用户在每个区最多只能创建2个账号", exception.getMessage());
    }
  }

  @Test
  void testCreateAccount_DuplicateAccountName() {
    try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
      // Arrange
      Long userId = 1L;
      userContextMock.when(UserContext::getUserId).thenReturn(userId);

      GameAccount existingAccount = new GameAccount();
      existingAccount.setId(1L);
      existingAccount.setUserId(userId);
      existingAccount.setAccountName("TestAccount");

      when(userRepository.existsById(userId)).thenReturn(true);
      when(gameAccountRepository.countByUserIdAndServerId(userId, 1)).thenReturn(0L);
      when(gameAccountRepository.findByAccountNameAndServerIdAndUserId("TestAccount", 1, userId))
          .thenReturn(Optional.of(existingAccount));

      // Act & Assert
      BusinessException exception = assertThrows(BusinessException.class, () -> {
        gameAccountService.createGameAccount(request);
      });
      assertEquals("该区已存在同名账号", exception.getMessage());
    }
  }
}