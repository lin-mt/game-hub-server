package com.app.gamehub.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.app.gamehub.dto.CreateDynastyRequest;
import com.app.gamehub.dto.JoinDynastyRequest;
import com.app.gamehub.dto.UpdateDynastyRequest;
import com.app.gamehub.entity.*;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.*;
import com.app.gamehub.util.AllianceCodeGenerator;
import com.app.gamehub.util.UserContext;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynastyServiceTest {

    @Mock
    private DynastyRepository dynastyRepository;
    
    @Mock
    private DynastyPositionRepository dynastyPositionRepository;
    
    @Mock
    private PositionReservationRepository positionReservationRepository;
    
    @Mock
    private GameAccountRepository gameAccountRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private AllianceCodeGenerator codeGenerator;

    @InjectMocks
    private DynastyService dynastyService;

    private Long userId = 1L;
    private Integer serverId = 1;
    private String dynastyCode = "ABC123";

    @BeforeEach
    void setUp() {
        // 模拟用户上下文
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
        }
    }

    @Test
    void createDynasty_Success() {
        // Given
        CreateDynastyRequest request = new CreateDynastyRequest();
        request.setName("大汉王朝");
        request.setServerId(serverId);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(dynastyRepository.existsByEmperorIdAndServerId(userId, serverId)).thenReturn(false);
        when(codeGenerator.generateCode()).thenReturn(dynastyCode);
        when(dynastyRepository.existsByCode(dynastyCode)).thenReturn(false);
        
        Dynasty savedDynasty = new Dynasty();
        savedDynasty.setId(1L);
        savedDynasty.setName(request.getName());
        savedDynasty.setCode(dynastyCode);
        savedDynasty.setServerId(serverId);
        savedDynasty.setEmperorId(userId);
        savedDynasty.setReservationEnabled(false);
        
        when(dynastyRepository.save(any(Dynasty.class))).thenReturn(savedDynasty);

        // When
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            Dynasty result = dynastyService.createDynasty(request);

            // Then
            assertNotNull(result);
            assertEquals("大汉王朝", result.getName());
            assertEquals(dynastyCode, result.getCode());
            assertEquals(serverId, result.getServerId());
            assertEquals(userId, result.getEmperorId());
            assertFalse(result.getReservationEnabled());

            verify(dynastyRepository).save(any(Dynasty.class));
            verify(dynastyPositionRepository, times(2)).save(any(DynastyPosition.class)); // 两种官职类型
        }
    }

    @Test
    void createDynasty_UserNotExists_ThrowsException() {
        // Given
        CreateDynastyRequest request = new CreateDynastyRequest();
        request.setName("大汉王朝");
        request.setServerId(serverId);

        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> dynastyService.createDynasty(request));
            assertEquals("用户不存在", exception.getMessage());
        }
    }

    @Test
    void createDynasty_DynastyAlreadyExists_ThrowsException() {
        // Given
        CreateDynastyRequest request = new CreateDynastyRequest();
        request.setName("大汉王朝");
        request.setServerId(serverId);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(dynastyRepository.existsByEmperorIdAndServerId(userId, serverId)).thenReturn(true);

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> dynastyService.createDynasty(request));
            assertEquals("您在该区已创建王朝，每个用户在每个区只能创建一个王朝", exception.getMessage());
        }
    }

    @Test
    void getDynastyByCode_Success() {
        // Given
        Dynasty dynasty = new Dynasty();
        dynasty.setId(1L);
        dynasty.setCode(dynastyCode);
        dynasty.setName("大汉王朝");

        when(dynastyRepository.findByCode(dynastyCode)).thenReturn(Optional.of(dynasty));

        // When
        Dynasty result = dynastyService.getDynastyByCode(dynastyCode);

        // Then
        assertNotNull(result);
        assertEquals(dynastyCode, result.getCode());
        assertEquals("大汉王朝", result.getName());
    }

    @Test
    void getDynastyByCode_NotFound_ThrowsException() {
        // Given
        when(dynastyRepository.findByCode(dynastyCode)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> dynastyService.getDynastyByCode(dynastyCode));
        assertEquals("王朝不存在", exception.getMessage());
    }

    @Test
    void joinDynasty_Success() {
        // Given
        Long accountId = 1L;
        JoinDynastyRequest request = new JoinDynastyRequest();
        request.setDynastyCode(dynastyCode);

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setServerId(serverId);
        account.setDynastyId(null);

        Dynasty dynasty = new Dynasty();
        dynasty.setId(1L);
        dynasty.setCode(dynastyCode);
        dynasty.setServerId(serverId);

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(dynastyRepository.findByCode(dynastyCode)).thenReturn(Optional.of(dynasty));
        when(gameAccountRepository.save(any(GameAccount.class))).thenReturn(account);

        // When
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            GameAccount result = dynastyService.joinDynasty(accountId, request);

            // Then
            assertNotNull(result);
            assertEquals(dynasty.getId(), result.getDynastyId());
            verify(gameAccountRepository).save(account);
        }
    }

    @Test
    void joinDynasty_AccountNotBelongsToUser_ThrowsException() {
        // Given
        Long accountId = 1L;
        Long otherUserId = 2L;
        JoinDynastyRequest request = new JoinDynastyRequest();
        request.setDynastyCode(dynastyCode);

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(otherUserId); // 不同的用户ID

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> dynastyService.joinDynasty(accountId, request));
            assertEquals("无权操作此账号", exception.getMessage());
        }
    }

    @Test
    void joinDynasty_DifferentServer_ThrowsException() {
        // Given
        Long accountId = 1L;
        JoinDynastyRequest request = new JoinDynastyRequest();
        request.setDynastyCode(dynastyCode);

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setServerId(1);
        account.setDynastyId(null);

        Dynasty dynasty = new Dynasty();
        dynasty.setId(1L);
        dynasty.setCode(dynastyCode);
        dynasty.setServerId(2); // 不同的区号

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(dynastyRepository.findByCode(dynastyCode)).thenReturn(Optional.of(dynasty));

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> dynastyService.joinDynasty(accountId, request));
            assertEquals("账号只能加入同一个区的王朝", exception.getMessage());
        }
    }

    @Test
    void validateEmperor_Success() {
        // Given
        Long dynastyId = 1L;
        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setEmperorId(userId);

        when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            assertDoesNotThrow(() -> dynastyService.validateEmperor(dynastyId));
        }
    }

    @Test
    void validateEmperor_NotEmperor_ThrowsException() {
        // Given
        Long dynastyId = 1L;
        Long otherUserId = 2L;
        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setEmperorId(otherUserId);

        when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);
            
            BusinessException exception = assertThrows(BusinessException.class,
                () -> dynastyService.validateEmperor(dynastyId));
            assertEquals("只有天子才能执行此操作", exception.getMessage());
        }
    }

    @Test
    void updateDynasty_Success() {
        // Given
        Long dynastyId = 1L;
        UpdateDynastyRequest request = new UpdateDynastyRequest();
        request.setName("新王朝名称");

        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setName("旧王朝名称");
        dynasty.setEmperorId(userId);

        Dynasty updatedDynasty = new Dynasty();
        updatedDynasty.setId(dynastyId);
        updatedDynasty.setName(request.getName());
        updatedDynasty.setEmperorId(userId);

        when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));
        when(dynastyRepository.save(any(Dynasty.class))).thenReturn(updatedDynasty);

        // When
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);

            Dynasty result = dynastyService.updateDynasty(dynastyId, request);

            // Then
            assertNotNull(result);
            assertEquals(request.getName(), result.getName());
            verify(dynastyRepository).save(dynasty);
            assertEquals(request.getName(), dynasty.getName());
        }
    }

    @Test
    void updateDynasty_NotEmperor_ThrowsException() {
        // Given
        Long dynastyId = 1L;
        Long otherUserId = 2L;
        UpdateDynastyRequest request = new UpdateDynastyRequest();
        request.setName("新王朝名称");

        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setEmperorId(otherUserId);

        when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> dynastyService.updateDynasty(dynastyId, request));
            assertEquals("只有天子才能执行此操作", exception.getMessage());
        }
    }

    @Test
    void deleteDynasty_Success() {
        // Given
        Long dynastyId = 1L;
        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setName("测试王朝");
        dynasty.setEmperorId(userId);

        GameAccount member1 = new GameAccount();
        member1.setId(1L);
        member1.setDynastyId(dynastyId);
        member1.setAccountName("成员1");

        GameAccount member2 = new GameAccount();
        member2.setId(2L);
        member2.setDynastyId(dynastyId);
        member2.setAccountName("成员2");

        List<GameAccount> members = Arrays.asList(member1, member2);

        DynastyPosition position1 = new DynastyPosition();
        position1.setId(1L);
        position1.setDynastyId(dynastyId);

        DynastyPosition position2 = new DynastyPosition();
        position2.setId(2L);
        position2.setDynastyId(dynastyId);

        List<DynastyPosition> positions = Arrays.asList(position1, position2);

        when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));

        // When
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);

            dynastyService.deleteDynasty(dynastyId);

            // Verify the deletion order: reservations -> positions -> accounts -> dynasty
            verify(positionReservationRepository).deleteByDynastyId(dynastyId);
            verify(positionReservationRepository).flush();
            
            verify(dynastyPositionRepository).deleteByDynastyId(dynastyId);
            verify(dynastyPositionRepository).flush();

            verify(gameAccountRepository).clearDynastyIdByDynastyId(dynastyId);
            verify(gameAccountRepository).flush();

            verify(dynastyRepository).delete(dynasty);
        }
    }

    @Test
    void deleteDynasty_NotEmperor_ThrowsException() {
        // Given
        Long dynastyId = 1L;
        Long otherUserId = 2L;
        Dynasty dynasty = new Dynasty();
        dynasty.setId(dynastyId);
        dynasty.setEmperorId(otherUserId);

        when(dynastyRepository.findById(dynastyId)).thenReturn(Optional.of(dynasty));

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> dynastyService.deleteDynasty(dynastyId));
            assertEquals("只有天子才能执行此操作", exception.getMessage());
        }
    }

    @Test
    void leaveDynasty_Success() {
        // Given
        Long accountId = 1L;
        Long dynastyId = 1L;

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setAccountName("测试账号");
        account.setDynastyId(dynastyId);

        GameAccount savedAccount = new GameAccount();
        savedAccount.setId(accountId);
        savedAccount.setUserId(userId);
        savedAccount.setAccountName("测试账号");
        savedAccount.setDynastyId(null);

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(gameAccountRepository.save(any(GameAccount.class))).thenReturn(savedAccount);

        // When
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);

            GameAccount result = dynastyService.leaveDynasty(accountId);

            // Then
            assertNotNull(result);
            assertNull(result.getDynastyId());
            verify(gameAccountRepository).save(account);
            verify(positionReservationRepository).deleteByAccountId(accountId);
            assertNull(account.getDynastyId());
        }
    }

    @Test
    void leaveDynasty_AccountNotFound_ThrowsException() {
        // Given
        Long accountId = 1L;
        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> dynastyService.leaveDynasty(accountId));
            assertEquals("账号不存在", exception.getMessage());
        }
    }

    @Test
    void leaveDynasty_NotAccountOwner_ThrowsException() {
        // Given
        Long accountId = 1L;
        Long otherUserId = 2L;

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(otherUserId);
        account.setDynastyId(1L);

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> dynastyService.leaveDynasty(accountId));
            assertEquals("无权操作此账号", exception.getMessage());
        }
    }

    @Test
    void leaveDynasty_NotInDynasty_ThrowsException() {
        // Given
        Long accountId = 1L;

        GameAccount account = new GameAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setDynastyId(null);

        when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // When & Then
        try (var mockedStatic = mockStatic(UserContext.class)) {
            mockedStatic.when(UserContext::getUserId).thenReturn(userId);

            BusinessException exception = assertThrows(BusinessException.class,
                () -> dynastyService.leaveDynasty(accountId));
            assertEquals("账号未加入任何王朝", exception.getMessage());
        }
    }
}
