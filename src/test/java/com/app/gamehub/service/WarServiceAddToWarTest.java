package com.app.gamehub.service;

import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.WarApplication;
import com.app.gamehub.entity.WarArrangement;
import com.app.gamehub.entity.WarType;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.repository.TacticTemplateRepository;
import com.app.gamehub.repository.WarApplicationRepository;
import com.app.gamehub.repository.WarArrangementRepository;
import com.app.gamehub.repository.WarGroupRepository;
import com.app.gamehub.util.UserContext;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarServiceAddToWarTest {

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
  @Mock
  private AllianceService allianceService;

  @InjectMocks
  private WarService warService;

  @AfterEach
  void tearDown() {
    UserContext.clear();
  }

  @Test
  void addToWar_GuanduMainLimitReached_ThrowsException() {
    Long accountId = 1L;
    Long allianceId = 10L;
    Long leaderId = 100L;

    UserContext.setUserId(leaderId);

    GameAccount account = new GameAccount();
    account.setId(accountId);
    account.setAllianceId(allianceId);

    Alliance alliance = new Alliance();
    alliance.setId(allianceId);
    alliance.setLeaderId(leaderId);
    alliance.setGuanduOneMainLimit(1);
    alliance.setGuanduOneSubLimit(1);
    alliance.setGuanduTwoMainLimit(30);
    alliance.setGuanduTwoSubLimit(10);

    when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(allianceRepository.findById(allianceId)).thenReturn(Optional.of(alliance));
    when(warArrangementRepository.findByAccountIdAndWarTypeIn(
        accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)))
        .thenReturn(Collections.emptyList());
    when(warArrangementRepository.countByAllianceIdAndWarTypeAndIsSubstitute(
        allianceId, WarType.GUANDU_ONE, false)).thenReturn(1L);
    when(warApplicationRepository.countByAllianceIdAndWarTypeAndStatusAndIsSubstitute(
        allianceId, WarType.GUANDU_ONE, WarApplication.ApplicationStatus.PENDING, false))
        .thenReturn(0L);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> warService.addToWar(accountId, WarType.GUANDU_ONE, false));
    assertEquals("官渡一主力人数已达上限（1人）", ex.getMessage());

    verify(warApplicationRepository, never()).save(any(WarApplication.class));
    verify(warArrangementRepository, never()).save(any(WarArrangement.class));
  }

  @Test
  void addToWar_GuanduWithinLimit_SavesApprovedApplicationAndArrangement() {
    Long accountId = 2L;
    Long allianceId = 20L;
    Long leaderId = 200L;

    UserContext.setUserId(leaderId);

    GameAccount account = new GameAccount();
    account.setId(accountId);
    account.setAllianceId(allianceId);

    Alliance alliance = new Alliance();
    alliance.setId(allianceId);
    alliance.setLeaderId(leaderId);
    alliance.setGuanduOneMainLimit(2);
    alliance.setGuanduOneSubLimit(1);
    alliance.setGuanduTwoMainLimit(30);
    alliance.setGuanduTwoSubLimit(10);

    when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(allianceRepository.findById(allianceId)).thenReturn(Optional.of(alliance));
    when(warArrangementRepository.findByAccountIdAndWarTypeIn(
        accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)))
        .thenReturn(Collections.emptyList());
    when(warArrangementRepository.countByAllianceIdAndWarTypeAndIsSubstitute(
        allianceId, WarType.GUANDU_ONE, false)).thenReturn(1L);
    when(warApplicationRepository.countByAllianceIdAndWarTypeAndStatusAndIsSubstitute(
        allianceId, WarType.GUANDU_ONE, WarApplication.ApplicationStatus.PENDING, false))
        .thenReturn(0L);
    when(warApplicationRepository.save(any(WarApplication.class))).thenAnswer(inv -> inv.getArgument(0));
    when(warArrangementRepository.save(any(WarArrangement.class))).thenAnswer(inv -> inv.getArgument(0));

    warService.addToWar(accountId, WarType.GUANDU_ONE, null);

    ArgumentCaptor<WarApplication> appCaptor = ArgumentCaptor.forClass(WarApplication.class);
    verify(warApplicationRepository).save(appCaptor.capture());
    assertEquals(WarApplication.ApplicationStatus.APPROVED, appCaptor.getValue().getStatus());
    assertEquals(false, appCaptor.getValue().getIsSubstitute());

    ArgumentCaptor<WarArrangement> arrangementCaptor = ArgumentCaptor.forClass(WarArrangement.class);
    verify(warArrangementRepository).save(arrangementCaptor.capture());
    assertEquals(false, arrangementCaptor.getValue().getIsSubstitute());
    assertEquals(WarType.GUANDU_ONE, arrangementCaptor.getValue().getWarType());
  }
}

