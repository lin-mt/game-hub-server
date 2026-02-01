package com.app.gamehub.service;

import com.app.gamehub.dto.UseTacticRequest;
import com.app.gamehub.entity.*;
import com.app.gamehub.repository.*;
import com.app.gamehub.util.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WarServiceGuanduCleanupTest {

    @Mock
    private WarApplicationRepository warApplicationRepository;
    @Mock
    private GameAccountRepository gameAccountRepository;
    @Mock
    private AllianceRepository allianceRepository;
    @Mock
    private WarArrangementRepository warArrangementRepository;
    @Mock
    private WarGroupRepository warGroupRepository;
    @Mock
    private TacticTemplateRepository tacticTemplateRepository;

    @InjectMocks
    private WarService warService;

    private Alliance testAlliance;
    private GameAccount testAccount1;
    private GameAccount testAccount2;
    private WarApplication testApplication1;
    private WarApplication testApplication2;
    private WarArrangement guanduOneArrangement;
    private WarArrangement guanduTwoArrangement;

    @BeforeEach
    void setUp() {
        // 创建测试联盟
        testAlliance = new Alliance();
        testAlliance.setId(1L);
        testAlliance.setLeaderId(100L);

        // 创建测试账号
        testAccount1 = new GameAccount();
        testAccount1.setId(1L);
        testAccount1.setAccountName("Account1");
        testAccount1.setDamageBonus(new BigDecimal("50.0"));

        testAccount2 = new GameAccount();
        testAccount2.setId(2L);
        testAccount2.setAccountName("Account2");
        testAccount2.setDamageBonus(new BigDecimal("40.0"));

        // 创建测试申请
        testApplication1 = new WarApplication();
        testApplication1.setAccountId(1L);
        testApplication1.setWarType(WarType.GUANDU_ONE);
        testApplication1.setStatus(WarApplication.ApplicationStatus.APPROVED);
        testApplication1.setCreatedAt(LocalDateTime.now().minusHours(2));

        testApplication2 = new WarApplication();
        testApplication2.setAccountId(2L);
        testApplication2.setWarType(WarType.GUANDU_TWO);
        testApplication2.setStatus(WarApplication.ApplicationStatus.APPROVED);
        testApplication2.setCreatedAt(LocalDateTime.now().minusHours(1));

        // 创建现有的战事安排（模拟移动后的状态）
        guanduOneArrangement = new WarArrangement();
        guanduOneArrangement.setId(1L);
        guanduOneArrangement.setAccountId(2L); // Account2 移动到了官渡一
        guanduOneArrangement.setAllianceId(1L);
        guanduOneArrangement.setWarType(WarType.GUANDU_ONE);
        guanduOneArrangement.setWarGroupId(null); // 移动后分组被清除

        guanduTwoArrangement = new WarArrangement();
        guanduTwoArrangement.setId(2L);
        guanduTwoArrangement.setAccountId(1L); // Account1 移动到了官渡二
        guanduTwoArrangement.setAllianceId(1L);
        guanduTwoArrangement.setWarType(WarType.GUANDU_TWO);
        guanduTwoArrangement.setWarGroupId(null); // 移动后分组被清除
    }

    @Test
    void testUseTacticGuanduOne_OnlyClearsGuanduOneData() {
        // Given
        Long allianceId = 1L;
        UseTacticRequest request = new UseTacticRequest();
        request.setWarType(WarType.GUANDU_ONE);
        request.setTactic("TACTIC_ONE");

        List<WarArrangement> guanduOneArrangements = Arrays.asList(guanduOneArrangement);

        // Mock repository calls
        doReturn(Optional.of(testAlliance)).when(allianceRepository).findById(allianceId);
        doReturn(guanduOneArrangements).when(warArrangementRepository)
            .findByAllianceIdAndWarTypeOrderByCreatedAtDesc(allianceId, WarType.GUANDU_ONE);
        doReturn(Arrays.asList(testApplication1)).when(warApplicationRepository)
            .findByAllianceIdAndWarTypeAndStatusOrderByCreatedAtAsc(
                allianceId, WarType.GUANDU_ONE, WarApplication.ApplicationStatus.APPROVED);
        doReturn(Arrays.asList(testAccount1)).when(gameAccountRepository).findAllById(anyList());
        doReturn(Arrays.asList(testApplication1)).when(warApplicationRepository)
            .findByAccountIdInAndWarTypeAndStatus(
                anyList(), eq(WarType.GUANDU_ONE), eq(WarApplication.ApplicationStatus.APPROVED));
        doReturn(Optional.empty()).when(tacticTemplateRepository).findByTacticKey("TACTIC_ONE");

        // When
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(100L);
            
            warService.useTactic(allianceId, request);
        }

        // Then
        // 验证只删除了官渡一的战事安排
        verify(warArrangementRepository).deleteAll(guanduOneArrangements);
        
        // 验证只删除了官渡一的战事分组
        verify(warGroupRepository).deleteByAllianceIdAndWarType(allianceId, WarType.GUANDU_ONE);
        
        // 验证获取了申请信息
        verify(warApplicationRepository).findByAllianceIdAndWarTypeAndStatusOrderByCreatedAtAsc(
            allianceId, WarType.GUANDU_ONE, WarApplication.ApplicationStatus.APPROVED);
    }

    @Test
    void testUseTacticGuanduTwo_OnlyClearsGuanduTwoData() {
        // Given
        Long allianceId = 1L;
        UseTacticRequest request = new UseTacticRequest();
        request.setWarType(WarType.GUANDU_TWO);
        request.setTactic("TACTIC_ONE");

        List<WarArrangement> guanduTwoArrangements = Arrays.asList(guanduTwoArrangement);

        // Mock repository calls
        doReturn(Optional.of(testAlliance)).when(allianceRepository).findById(allianceId);
        doReturn(guanduTwoArrangements).when(warArrangementRepository)
            .findByAllianceIdAndWarTypeOrderByCreatedAtDesc(allianceId, WarType.GUANDU_TWO);
        doReturn(Arrays.asList(testApplication2)).when(warApplicationRepository)
            .findByAllianceIdAndWarTypeAndStatusOrderByCreatedAtAsc(
                allianceId, WarType.GUANDU_TWO, WarApplication.ApplicationStatus.APPROVED);
        doReturn(Arrays.asList(testAccount2)).when(gameAccountRepository).findAllById(anyList());
        doReturn(Arrays.asList(testApplication2)).when(warApplicationRepository)
            .findByAccountIdInAndWarTypeAndStatus(
                anyList(), eq(WarType.GUANDU_TWO), eq(WarApplication.ApplicationStatus.APPROVED));
        doReturn(Optional.empty()).when(tacticTemplateRepository).findByTacticKey("TACTIC_ONE");

        // When
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(100L);
            
            warService.useTactic(allianceId, request);
        }

        // Then
        // 验证只删除了官渡二的战事安排
        verify(warArrangementRepository).deleteAll(guanduTwoArrangements);
        
        // 验证只删除了官渡二的战事分组
        verify(warGroupRepository).deleteByAllianceIdAndWarType(allianceId, WarType.GUANDU_TWO);
        
        // 验证获取了申请信息
        verify(warApplicationRepository).findByAllianceIdAndWarTypeAndStatusOrderByCreatedAtAsc(
            allianceId, WarType.GUANDU_TWO, WarApplication.ApplicationStatus.APPROVED);
    }
}