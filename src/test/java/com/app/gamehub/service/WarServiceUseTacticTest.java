package com.app.gamehub.service;

import com.app.gamehub.dto.UseTacticRequest;
import com.app.gamehub.entity.*;
import com.app.gamehub.model.WarTactic;
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
class WarServiceUseTacticTest {

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

    @InjectMocks
    private WarService warService;

    private Alliance testAlliance;
    private GameAccount testAccount1;
    private GameAccount testAccount2;
    private GameAccount testAccount3;
    private WarApplication testApplication1;
    private WarApplication testApplication2;
    private WarApplication testApplication3;

    @BeforeEach
    void setUp() {
        // 创建测试联盟
        testAlliance = new Alliance();
        testAlliance.setId(1L);
        testAlliance.setLeaderId(100L);

        // 创建测试账号（按伤害加成从高到低）
        testAccount1 = new GameAccount();
        testAccount1.setId(1L);
        testAccount1.setAccountName("Account1");
        testAccount1.setDamageBonus(new BigDecimal("50.0"));

        testAccount2 = new GameAccount();
        testAccount2.setId(2L);
        testAccount2.setAccountName("Account2");
        testAccount2.setDamageBonus(new BigDecimal("40.0"));

        testAccount3 = new GameAccount();
        testAccount3.setId(3L);
        testAccount3.setAccountName("Account3");
        testAccount3.setDamageBonus(new BigDecimal("30.0"));

        // 创建测试申请（按申请时间排序：Account3最早，Account1最晚）
        testApplication1 = new WarApplication();
        testApplication1.setAccountId(1L);
        testApplication1.setCreatedAt(LocalDateTime.now().minusHours(1)); // 最晚申请

        testApplication2 = new WarApplication();
        testApplication2.setAccountId(2L);
        testApplication2.setCreatedAt(LocalDateTime.now().minusHours(2)); // 中间申请

        testApplication3 = new WarApplication();
        testApplication3.setAccountId(3L);
        testApplication3.setCreatedAt(LocalDateTime.now().minusHours(3)); // 最早申请
    }

    @Test
    void testUseTacticWithGuanduWarSortsByApplicationTime() {
        // Given
        Long allianceId = 1L;
        UseTacticRequest request = new UseTacticRequest();
        request.setWarType(WarType.GUANDU_ONE);
        request.setTactic("TACTIC_ONE");

        List<WarArrangement> existingArrangements = Arrays.asList(
            createWarArrangement(1L, allianceId),
            createWarArrangement(2L, allianceId),
            createWarArrangement(3L, allianceId)
        );

        // Mock repository calls
        doReturn(Optional.of(testAlliance)).when(allianceRepository).findById(allianceId);
        doReturn(existingArrangements).when(warArrangementRepository)
            .findByAllianceIdAndWarTypeOrderByCreatedAtDesc(allianceId, WarType.GUANDU_ONE);
        doReturn(Arrays.asList(testAccount1, testAccount2, testAccount3)).when(gameAccountRepository).findAllById(anyList());
        doReturn(Arrays.asList(testApplication1, testApplication2, testApplication3))
            .when(warApplicationRepository).findByAccountIdInAndWarTypeAndStatus(
                anyList(), eq(WarType.GUANDU_ONE), eq(WarApplication.ApplicationStatus.APPROVED));

        // When
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(100L);
            
            warService.useTactic(allianceId, request);
        }

        // Then
        // 验证删除了现有的安排
        verify(warArrangementRepository).deleteAll(existingArrangements);
        verify(warGroupRepository).deleteByAllianceIdAndWarType(allianceId, WarType.GUANDU_ONE);
        
        // 验证获取了申请时间信息（使用 anyCollection 因为实际传入的是 HashSet）
        verify(warApplicationRepository).findByAccountIdInAndWarTypeAndStatus(
            anyCollection(), eq(WarType.GUANDU_ONE), eq(WarApplication.ApplicationStatus.APPROVED));
    }

    private WarArrangement createWarArrangement(Long accountId, Long allianceId) {
        WarArrangement arrangement = new WarArrangement();
        arrangement.setAccountId(accountId);
        arrangement.setAllianceId(allianceId);
        arrangement.setWarType(WarType.GUANDU_ONE);
        return arrangement;
    }
}
