package com.app.gamehub.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.app.gamehub.dto.ReservePositionRequest;
import com.app.gamehub.dto.SetPositionReservationTimeRequest;
import com.app.gamehub.entity.*;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.DynastyPositionRepository;
import com.app.gamehub.repository.DynastyRepository;
import com.app.gamehub.repository.GameAccountRepository;
import com.app.gamehub.repository.PositionReservationRepository;
import com.app.gamehub.util.UserContext;
import com.app.gamehub.util.Utils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PositionReservationServiceTest {

    @Mock
    private DynastyRepository dynastyRepository;
    
    @Mock
    private DynastyPositionRepository dynastyPositionRepository;
    
    @Mock
    private PositionReservationRepository positionReservationRepository;
    
    @Mock
    private GameAccountRepository gameAccountRepository;

    @InjectMocks
    private PositionReservationService positionReservationService;

    private Long userId = 1L;
    private Long dynastyId = 1L;
    private Long accountId = 1L;

    @BeforeEach
    void setUp() {
        // 设置通用的mock行为 - 使用lenient避免不必要的stubbing警告
        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setEmperorId(userId);
        lenient().when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));
    }

    @Test
    void setPositionReservationTime_Success() {
        // Given
        SetPositionReservationTimeRequest request = new SetPositionReservationTimeRequest();
        request.setPositionType(PositionType.TAI_WEI);
        request.setReservationStartTime(LocalDateTime.of(2025, 8, 11, 10, 0, 0));
        request.setReservationEndTime(LocalDateTime.of(2025, 8, 11, 23, 0, 0));
        request.setDutyDate(LocalDate.of(2025, 8, 12));
        request.setDisabledTimeSlots(Arrays.asList(0, 1, 2));

        DynastyPosition position = new DynastyPosition();
        position.setDynastyId(dynastyId);
        position.setPositionType(PositionType.TAI_WEI);

        when(dynastyPositionRepository.findByDynastyIdAndPositionType(dynastyId, PositionType.TAI_WEI))
                .thenReturn(Optional.of(position));
        when(dynastyPositionRepository.save(any(DynastyPosition.class))).thenReturn(position);

        // When
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            DynastyPosition result = positionReservationService.setPositionReservationTime(dynastyId, request);

            // Then
            assertNotNull(result);
            verify(dynastyPositionRepository).save(position);
            verify(positionReservationRepository).deleteByDutyDateBefore(request.getDutyDate().minusDays(2));
        }
    }

    @Test
    void setPositionReservationTime_InvalidTimeRange_ThrowsException() {
        // Given
        SetPositionReservationTimeRequest request = new SetPositionReservationTimeRequest();
        request.setPositionType(PositionType.TAI_WEI);
        request.setReservationStartTime(LocalDateTime.of(2025, 8, 11, 23, 0, 0)); // 开始时间晚于结束时间
        request.setReservationEndTime(LocalDateTime.of(2025, 8, 11, 10, 0, 0));
        request.setDutyDate(LocalDate.of(2025, 8, 12));

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> positionReservationService.setPositionReservationTime(dynastyId, request));
            
            assertEquals("预约开始时间必须早于结束时间", exception.getMessage());
        }
    }

    @Test
    void setPositionReservationTime_DifferentDays_ThrowsException() {
        // Given
        SetPositionReservationTimeRequest request = new SetPositionReservationTimeRequest();
        request.setPositionType(PositionType.TAI_WEI);
        request.setReservationStartTime(LocalDateTime.of(2025, 8, 11, 10, 0, 0));
        request.setReservationEndTime(LocalDateTime.of(2025, 8, 12, 10, 0, 0)); // 不同天
        request.setDutyDate(LocalDate.of(2025, 8, 13));

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> positionReservationService.setPositionReservationTime(dynastyId, request));
            
            assertEquals("预约开始时间和结束时间必须在同一天", exception.getMessage());
        }
    }

    @Test
    void setPositionReservationTime_DutyDateNotAfterReservationDate_ThrowsException() {
        // Given
        SetPositionReservationTimeRequest request = new SetPositionReservationTimeRequest();
        request.setPositionType(PositionType.TAI_WEI);
        request.setReservationStartTime(LocalDateTime.of(2025, 8, 11, 10, 0, 0));
        request.setReservationEndTime(LocalDateTime.of(2025, 8, 11, 23, 0, 0));
        request.setDutyDate(LocalDate.of(2025, 8, 11)); // 任职日期不在预约时间之后

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> positionReservationService.setPositionReservationTime(dynastyId, request));
            
            assertEquals("任职日期必须在预约时间之后", exception.getMessage());
        }
    }

    @Test
    void reservePosition_Success() {
        // Given
        ReservePositionRequest request = new ReservePositionRequest();
        request.setPositionType(PositionType.TAI_WEI);
        request.setTimeSlot(10);

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setDynastyId(dynastyId);

        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setReservationEnabled(true);

        DynastyPosition position = new DynastyPosition();
        position.setDynastyId(dynastyId);
        position.setPositionType(PositionType.TAI_WEI);
        position.setReservationStartTime(LocalDateTime.now().minusHours(1)); // 1小时前开始
        position.setReservationEndTime(LocalDateTime.now().plusHours(1)); // 1小时后结束
        position.setDutyDate(LocalDate.now().plusDays(1));
        position.setDisabledTimeSlots("");

        PositionReservation savedReservation = new PositionReservation();
        savedReservation.setId(1L);
        savedReservation.setDynastyId(dynastyId);
        savedReservation.setPositionType(PositionType.TAI_WEI);
        savedReservation.setTimeSlot(10);
        savedReservation.setAccountId(accountId);

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));
        when(dynastyPositionRepository.findByDynastyIdAndPositionType(dynastyId, PositionType.TAI_WEI))
                .thenReturn(Optional.of(position));
        when(positionReservationRepository.existsByAccountIdAndDynastyIdAndPositionTypeAndDutyDate(
                accountId, dynastyId, PositionType.TAI_WEI, position.getDutyDate())).thenReturn(false);
        when(positionReservationRepository.existsByDynastyIdAndPositionTypeAndDutyDateAndTimeSlot(
                dynastyId, PositionType.TAI_WEI, position.getDutyDate(), 10)).thenReturn(false);
        when(positionReservationRepository.save(any(PositionReservation.class))).thenReturn(savedReservation);

        // When
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            PositionReservation result = positionReservationService.reservePosition(dynastyId, accountId, request);

            // Then
            assertNotNull(result);
            assertEquals(dynastyId, result.getDynastyId());
            assertEquals(PositionType.TAI_WEI, result.getPositionType());
            assertEquals(10, result.getTimeSlot());
            assertEquals(accountId, result.getAccountId());
            verify(positionReservationRepository).save(any(PositionReservation.class));
        }
    }

    @Test
    void reservePosition_AccountNotBelongsToUser_ThrowsException() {
        // Given
        ReservePositionRequest request = new ReservePositionRequest();
        request.setPositionType(PositionType.TAI_WEI);
        request.setTimeSlot(10);

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(999L); // 不同的用户ID

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        
        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> positionReservationService.reservePosition(dynastyId, accountId, request));
            
            assertEquals("无权操作此账号", exception.getMessage());
        }
    }

    @Test
    void reservePosition_AccountNotInDynasty_ThrowsException() {
        // Given
        ReservePositionRequest request = new ReservePositionRequest();
        request.setPositionType(PositionType.TAI_WEI);
        request.setTimeSlot(10);

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setDynastyId(999L); // 不同的王朝ID

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        
        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> positionReservationService.reservePosition(dynastyId, accountId, request));
            
            assertEquals("账号不属于该王朝", exception.getMessage());
        }
    }

    @Test
    void reservePosition_ReservationNotEnabled_ThrowsException() {
        // Given
        ReservePositionRequest request = new ReservePositionRequest();
        request.setPositionType(PositionType.TAI_WEI);
        request.setTimeSlot(10);

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setDynastyId(dynastyId);

        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setReservationEnabled(false); // 未开启预约

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));
        
        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> positionReservationService.reservePosition(dynastyId, accountId, request));
            
            assertEquals("王朝未开启官职预约", exception.getMessage());
        }
    }

    @Test
    void reservePosition_OutsideReservationTime_ThrowsException() {
        // Given
        ReservePositionRequest request = new ReservePositionRequest();
        request.setPositionType(PositionType.TAI_WEI);
        request.setTimeSlot(10);

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setDynastyId(dynastyId);

        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setReservationEnabled(true);

        DynastyPosition position = new DynastyPosition();
        position.setDynastyId(dynastyId);
        position.setPositionType(PositionType.TAI_WEI);
        position.setReservationStartTime(LocalDateTime.now().plusHours(1)); // 1小时后开始
        position.setReservationEndTime(LocalDateTime.now().plusHours(2)); // 2小时后结束
        position.setDutyDate(LocalDate.now().plusDays(1));

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));
        when(dynastyPositionRepository.findByDynastyIdAndPositionType(dynastyId, PositionType.TAI_WEI))
                .thenReturn(Optional.of(position));
        
        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class);
             var mockedUtils = mockStatic(Utils.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            mockedUtils.when(() -> Utils.format(any(LocalDateTime.class))).thenReturn("formatted time");
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> positionReservationService.reservePosition(dynastyId, accountId, request));
            
            assertEquals("官职预约的时间为：%s～%s"
                .formatted(
                    Utils.format(position.getReservationStartTime()),
                    Utils.format(position.getReservationEndTime())), exception.getMessage());
        }
    }

    @Test
    void reservePosition_TimeSlotDisabled_ThrowsException() {
        // Given
        ReservePositionRequest request = new ReservePositionRequest();
        request.setPositionType(PositionType.TAI_WEI);
        request.setTimeSlot(10);

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setDynastyId(dynastyId);

        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setReservationEnabled(true);

        DynastyPosition position = new DynastyPosition();
        position.setDynastyId(dynastyId);
        position.setPositionType(PositionType.TAI_WEI);
        position.setReservationStartTime(LocalDateTime.now().minusHours(1));
        position.setReservationEndTime(LocalDateTime.now().plusHours(1));
        position.setDutyDate(LocalDate.now().plusDays(1));
        position.setDisabledTimeSlots("10,11,12"); // 时段10被禁用

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));
        when(dynastyPositionRepository.findByDynastyIdAndPositionType(dynastyId, PositionType.TAI_WEI))
                .thenReturn(Optional.of(position));
        
        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> positionReservationService.reservePosition(dynastyId, accountId, request));
            
            assertEquals("该时段已被禁用，无法预约", exception.getMessage());
        }
    }
}