package com.app.gamehub.service;

import com.app.gamehub.dto.GameAccountWithApplicationTime;
import com.app.gamehub.dto.WarArrangementResponse;
import com.app.gamehub.entity.*;
import com.app.gamehub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarGroupServiceTest {

    @Mock
    private WarGroupRepository warGroupRepository;

    @Mock
    private WarArrangementRepository warArrangementRepository;

    @Mock
    private AllianceRepository allianceRepository;

    @Mock
    private GameAccountRepository gameAccountRepository;

    @Mock
    private WarApplicationRepository warApplicationRepository;

    @Mock
    private AllianceNotificationService allianceNotificationService;

    @InjectMocks
    private WarGroupService warGroupService;

    private Alliance testAlliance;
    private GameAccount testAccount;
    private WarArrangement testArrangement;
    private WarApplication testApplication;

    @BeforeEach
    void setUp() {
        // Setup test data
        testAlliance = new Alliance();
        testAlliance.setId(1L);
        testAlliance.setName("Test Alliance");

        testAccount = new GameAccount();
        testAccount.setId(100L);
        testAccount.setAccountName("Test Account");
        testAccount.setPowerValue(1000000L);
        testAccount.setTroopQuantity(500L); // Test the new troop quantity field
        testAccount.setLvbuStarLevel(BigDecimal.valueOf(3.5));

        testArrangement = new WarArrangement();
        testArrangement.setId(1L);
        testArrangement.setAccountId(100L);
        testArrangement.setAllianceId(1L);
        testArrangement.setWarType(WarType.GUANDU_ONE);
        testArrangement.setWarGroupId(null); // Mobile member

        testApplication = new WarApplication();
        testApplication.setId(1L);
        testApplication.setAccountId(100L);
        testApplication.setAllianceId(1L);
        testApplication.setWarType(WarType.GUANDU_ONE);
        testApplication.setStatus(WarApplication.ApplicationStatus.APPROVED);
        testApplication.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void testGetWarArrangementDetailWithApplicationTime() {
        // Given
        Long allianceId = 1L;
        WarType warType = WarType.GUANDU_ONE;

        when(allianceRepository.findById(allianceId)).thenReturn(Optional.of(testAlliance));
        when(warGroupRepository.findByAllianceIdAndWarTypeOrderByCreatedAtAsc(allianceId, warType))
                .thenReturn(Arrays.asList());
        when(warArrangementRepository.findByAllianceIdAndWarTypeOrderByCreatedAtDesc(allianceId, warType))
                .thenReturn(Arrays.asList(testArrangement));
        when(gameAccountRepository.findAllById(Arrays.asList(100L)))
                .thenReturn(Arrays.asList(testAccount));
        when(warApplicationRepository.findByAccountIdInAndWarTypeAndStatus(
                Arrays.asList(100L), warType, WarApplication.ApplicationStatus.APPROVED))
                .thenReturn(Arrays.asList(testApplication));

        // When
        WarArrangementResponse response = warGroupService.getWarArrangementDetail(allianceId, warType);

        // Then
        assertNotNull(response);
        assertEquals(allianceId, response.getAllianceId());
        assertEquals(warType, response.getWarType());
        
        // Verify mobile members include application time for GuanDu wars
        List<GameAccountWithApplicationTime> mobileMembers = response.getMobileMembers();
        assertNotNull(mobileMembers);
        assertEquals(1, mobileMembers.size());
        
        GameAccountWithApplicationTime member = mobileMembers.get(0);
        assertEquals(testAccount.getId(), member.getId());
        assertEquals(testAccount.getAccountName(), member.getAccountName());
        assertEquals(testAccount.getTroopQuantity(), member.getTroopQuantity()); // Test new field
        assertEquals(testApplication.getCreatedAt(), member.getApplicationTime()); // Test application time
        
        // Verify repository calls
        verify(warApplicationRepository).findByAccountIdInAndWarTypeAndStatus(
                Arrays.asList(100L), warType, WarApplication.ApplicationStatus.APPROVED);
    }

    @Test
    void testGetWarArrangementDetailForNonGuanDuWar() {
        // Given
        Long allianceId = 1L;
        WarType warType = WarType.SIEGE; // Non-GuanDu war
        
        testArrangement.setWarType(warType);

        when(allianceRepository.findById(allianceId)).thenReturn(Optional.of(testAlliance));
        when(warGroupRepository.findByAllianceIdAndWarTypeOrderByCreatedAtAsc(allianceId, warType))
                .thenReturn(Arrays.asList());
        when(warArrangementRepository.findByAllianceIdAndWarTypeOrderByCreatedAtDesc(allianceId, warType))
                .thenReturn(Arrays.asList(testArrangement));
        when(gameAccountRepository.findAllById(Arrays.asList(100L)))
                .thenReturn(Arrays.asList(testAccount));

        // When
        WarArrangementResponse response = warGroupService.getWarArrangementDetail(allianceId, warType);

        // Then
        assertNotNull(response);
        
        // Verify mobile members don't have application time for non-GuanDu wars
        List<GameAccountWithApplicationTime> mobileMembers = response.getMobileMembers();
        assertNotNull(mobileMembers);
        assertEquals(1, mobileMembers.size());
        
        GameAccountWithApplicationTime member = mobileMembers.get(0);
        assertEquals(testAccount.getId(), member.getId());
        assertNull(member.getApplicationTime()); // Should be null for non-GuanDu wars
        
        // Verify war application repository is not called for non-GuanDu wars
        verify(warApplicationRepository, never()).findByAccountIdInAndWarTypeAndStatus(any(), any(), any());
    }
}
