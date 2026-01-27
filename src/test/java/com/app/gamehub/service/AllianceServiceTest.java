package com.app.gamehub.service;

import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.*;
import com.app.gamehub.util.AllianceCodeGenerator;
import com.app.gamehub.util.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllianceServiceTest {

    @Mock
    private AllianceRepository allianceRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private GameAccountRepository gameAccountRepository;
    
    @Mock
    private AllianceApplicationRepository allianceApplicationRepository;
    
    @Mock
    private WarApplicationRepository warApplicationRepository;
    
    @Mock
    private WarArrangementRepository warArrangementRepository;
    
    @Mock
    private WarGroupRepository warGroupRepository;

    @Mock
    private BarbarianGroupRepository barbarianGroupRepository;

    @Mock
    private CarriageQueueRepository carriageQueueRepository;

    @Mock
    private AllianceCodeGenerator codeGenerator;

    private AllianceService allianceService;

    private Long userId = 1L;
    private Long allianceId = 100L;
    private Alliance alliance;
    private GameAccount member1;
    private GameAccount member2;

    @BeforeEach
    void setUp() {
        // 手动创建AllianceService实例，确保所有依赖都被正确注入
        allianceService = new AllianceService(
            allianceRepository,
            userRepository,
            gameAccountRepository,
            allianceApplicationRepository,
            warApplicationRepository,
            warArrangementRepository,
            warGroupRepository,
            barbarianGroupRepository,
            carriageQueueRepository,
            codeGenerator
        );

        alliance = new Alliance();
        alliance.setId(allianceId);
        alliance.setName("测试联盟");
        alliance.setCode("TEST01");
        alliance.setServerId(1);
        alliance.setLeaderId(userId);

        member1 = new GameAccount();
        member1.setId(1L);
        member1.setUserId(2L);
        member1.setAllianceId(allianceId);
        member1.setMemberTier(GameAccount.MemberTier.TIER_1);

        member2 = new GameAccount();
        member2.setId(2L);
        member2.setUserId(3L);
        member2.setAllianceId(allianceId);
        member2.setMemberTier(GameAccount.MemberTier.TIER_2);
    }

    @Test
    void deleteAlliance_Success_WithMembers() {
        // Given
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(userId);

            when(allianceRepository.findById(allianceId)).thenReturn(Optional.of(alliance));

            // When
            allianceService.deleteAlliance(allianceId);

            // Then
            // 验证删除操作被调用（按正确的顺序）
            verify(warApplicationRepository).deleteAllByAllianceId(allianceId);
            verify(warApplicationRepository).flush();

            verify(warArrangementRepository).deleteByAllianceId(allianceId);
            verify(warArrangementRepository).flush();

            verify(warGroupRepository).deleteByAllianceId(allianceId);
            verify(warGroupRepository).flush();

            verify(allianceApplicationRepository).deleteAllByAllianceId(allianceId);
            verify(allianceApplicationRepository).flush();

            verify(gameAccountRepository).clearAllianceIdByAllianceId(allianceId);
            verify(gameAccountRepository).flush();

            verify(allianceRepository).delete(alliance);
        }
    }

    @Test
    void deleteAlliance_Success_WithoutMembers() {
        // Given
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(userId);

            when(allianceRepository.findById(allianceId)).thenReturn(Optional.of(alliance));

            // When
            allianceService.deleteAlliance(allianceId);

            // Then
            // 验证删除操作被调用（按正确的顺序）
            verify(warApplicationRepository).deleteAllByAllianceId(allianceId);
            verify(warApplicationRepository).flush();

            verify(warArrangementRepository).deleteByAllianceId(allianceId);
            verify(warArrangementRepository).flush();

            verify(warGroupRepository).deleteByAllianceId(allianceId);
            verify(warGroupRepository).flush();

            verify(allianceApplicationRepository).deleteAllByAllianceId(allianceId);
            verify(allianceApplicationRepository).flush();

            verify(gameAccountRepository).clearAllianceIdByAllianceId(allianceId);
            verify(gameAccountRepository).flush();

            verify(allianceRepository).delete(alliance);
        }
    }

    @Test
    void deleteAlliance_ThrowsException_WhenAllianceNotFound() {
        // Given
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(userId);
            
            when(allianceRepository.findById(allianceId)).thenReturn(Optional.empty());
            
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> allianceService.deleteAlliance(allianceId));
            assertEquals("联盟不存在", exception.getMessage());
            
            // 验证没有调用任何删除操作
            verify(allianceApplicationRepository, never()).deleteAllByAllianceId(any());
            verify(warApplicationRepository, never()).deleteAllByAllianceId(any());
            verify(warArrangementRepository, never()).deleteByAllianceId(any());
            verify(warGroupRepository, never()).deleteByAllianceId(any());
            verify(allianceRepository, never()).delete(any());
        }
    }

    @Test
    void deleteAlliance_ThrowsException_WhenNotLeader() {
        // Given
        Long otherUserId = 999L;
        
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(otherUserId);
            
            when(allianceRepository.findById(allianceId)).thenReturn(Optional.of(alliance));
            
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> allianceService.deleteAlliance(allianceId));
            assertEquals("只有盟主可以删除联盟", exception.getMessage());
            
            // 验证没有调用任何删除操作
            verify(allianceApplicationRepository, never()).deleteAllByAllianceId(any());
            verify(warApplicationRepository, never()).deleteAllByAllianceId(any());
            verify(warArrangementRepository, never()).deleteByAllianceId(any());
            verify(warGroupRepository, never()).deleteByAllianceId(any());
            verify(allianceRepository, never()).delete(any());
        }
    }
}
