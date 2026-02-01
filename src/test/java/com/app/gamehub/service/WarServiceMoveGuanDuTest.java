package com.app.gamehub.service;

import com.app.gamehub.entity.*;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarServiceMoveGuanDuTest {

    @Mock
    private GameAccountRepository gameAccountRepository;
    @Mock
    private WarArrangementRepository warArrangementRepository;
    @Mock
    private WarApplicationRepository warApplicationRepository;

    @InjectMocks
    private WarService warService;

    private GameAccount testAccount;
    private WarArrangement testArrangement;
    private WarApplication testApplication;

    @BeforeEach
    void setUp() {
        testAccount = new GameAccount();
        testAccount.setId(1L);
        testAccount.setAccountName("TestAccount");

        testArrangement = new WarArrangement();
        testArrangement.setId(1L);
        testArrangement.setAccountId(1L);
        testArrangement.setWarType(WarType.GUANDU_ONE);
        testArrangement.setWarGroupId(100L);
        
        testApplication = new WarApplication();
        testApplication.setId(1L);
        testApplication.setAccountId(1L);
        testApplication.setWarType(WarType.GUANDU_ONE);
        testApplication.setStatus(WarApplication.ApplicationStatus.APPROVED);
    }

    @Test
    void testMoveGuanDuWar_FromGuanduOneToGuanduTwo_ClearsGroupInfoAndUpdatesApplication() {
        // Given
        Long accountId = 1L;
        
        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(warArrangementRepository.findByAccountIdAndWarTypeIn(
            accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)))
            .thenReturn(Collections.singletonList(testArrangement));
        when(warApplicationRepository.findByAccountIdAndWarTypeIn(
            accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)))
            .thenReturn(Collections.singletonList(testApplication));

        // When
        WarArrangement result = warService.moveGuanDuWar(accountId);

        // Then
        assertEquals(WarType.GUANDU_TWO, result.getWarType());
        assertNull(result.getWarGroupId()); // 分组信息应该被清除
        
        // 验证保存了战事安排
        verify(warArrangementRepository).save(testArrangement);
        
        // 验证更新了战事申请的类型
        assertEquals(WarType.GUANDU_TWO, testApplication.getWarType());
        verify(warApplicationRepository).save(testApplication);
    }

    @Test
    void testMoveGuanDuWar_FromGuanduTwoToGuanduOne_ClearsGroupInfoAndUpdatesApplication() {
        // Given
        Long accountId = 1L;
        testArrangement.setWarType(WarType.GUANDU_TWO);
        testApplication.setWarType(WarType.GUANDU_TWO);
        
        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(warArrangementRepository.findByAccountIdAndWarTypeIn(
            accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)))
            .thenReturn(Collections.singletonList(testArrangement));
        when(warApplicationRepository.findByAccountIdAndWarTypeIn(
            accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)))
            .thenReturn(Collections.singletonList(testApplication));

        // When
        WarArrangement result = warService.moveGuanDuWar(accountId);

        // Then
        assertEquals(WarType.GUANDU_ONE, result.getWarType());
        assertNull(result.getWarGroupId()); // 分组信息应该被清除
        
        // 验证保存了战事安排
        verify(warArrangementRepository).save(testArrangement);
        
        // 验证更新了战事申请的类型
        assertEquals(WarType.GUANDU_ONE, testApplication.getWarType());
        verify(warApplicationRepository).save(testApplication);
    }

    @Test
    void testMoveGuanDuWar_WithoutGroup_OnlyChangesWarType() {
        // Given
        Long accountId = 1L;
        testArrangement.setWarGroupId(null); // 没有分组
        
        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(warArrangementRepository.findByAccountIdAndWarTypeIn(
            accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)))
            .thenReturn(Collections.singletonList(testArrangement));
        when(warApplicationRepository.findByAccountIdAndWarTypeIn(
            accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)))
            .thenReturn(Collections.singletonList(testApplication));

        // When
        WarArrangement result = warService.moveGuanDuWar(accountId);

        // Then
        assertEquals(WarType.GUANDU_TWO, result.getWarType());
        assertNull(result.getWarGroupId()); // 没有分组信息
        
        // 验证保存了战事安排
        verify(warArrangementRepository).save(testArrangement);
        
        // 验证更新了战事申请的类型
        assertEquals(WarType.GUANDU_TWO, testApplication.getWarType());
        verify(warApplicationRepository).save(testApplication);
    }

    @Test
    void testMoveGuanDuWar_AccountNotExists_ThrowsException() {
        // Given
        Long accountId = 1L;
        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> warService.moveGuanDuWar(accountId));
        assertEquals("账号不存在", exception.getMessage());
    }

    @Test
    void testMoveGuanDuWar_NoWarArrangement_ThrowsException() {
        // Given
        Long accountId = 1L;
        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(warArrangementRepository.findByAccountIdAndWarTypeIn(
            accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)))
            .thenReturn(Collections.emptyList());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> warService.moveGuanDuWar(accountId));
        assertEquals("账号不能同时参加官渡一和官渡二", exception.getMessage());
    }

    @Test
    void testMoveGuanDuWar_MultipleWarArrangements_ThrowsException() {
        // Given
        Long accountId = 1L;
        WarArrangement arrangement1 = new WarArrangement();
        arrangement1.setWarType(WarType.GUANDU_ONE);
        WarArrangement arrangement2 = new WarArrangement();
        arrangement2.setWarType(WarType.GUANDU_TWO);
        
        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(warArrangementRepository.findByAccountIdAndWarTypeIn(
            accountId, List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)))
            .thenReturn(Arrays.asList(arrangement1, arrangement2));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> warService.moveGuanDuWar(accountId));
        assertEquals("账号不能同时参加官渡一和官渡二", exception.getMessage());
    }
}