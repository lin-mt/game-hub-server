package com.app.gamehub.service;

import com.app.gamehub.dto.CreateGameAccountRequest;
import com.app.gamehub.dto.UpdateGameAccountRequest;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.repository.*;
import com.app.gamehub.util.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameAccountServiceTest {

    @Mock
    private GameAccountRepository gameAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AllianceRepository allianceRepository;

    @Mock
    private BarbarianGroupRepository barbarianGroupRepository;

    @Mock
    private WarApplicationRepository warApplicationRepository;

    @Mock
    private WarArrangementRepository warArrangementRepository;

    @Mock
    private AllianceApplicationRepository allianceApplicationRepository;

    @InjectMocks
    private GameAccountService gameAccountService;

    private CreateGameAccountRequest createRequest;
    private UpdateGameAccountRequest updateRequest;
    private GameAccount existingAccount;

    @BeforeEach
    void setUp() {
        createRequest = new CreateGameAccountRequest();
        createRequest.setServerId(1);
        createRequest.setAccountName("Test Account");
        createRequest.setPowerValue(1000000L);
        createRequest.setDamageBonus(BigDecimal.valueOf(50.25));
        createRequest.setTroopLevel(25);
        createRequest.setRallyCapacity(300);
        createRequest.setTroopQuantity(500L); // Test the new troop quantity field
        createRequest.setLvbuStarLevel(BigDecimal.valueOf(3.5));
        createRequest.setInfantryDefense(120);
        createRequest.setInfantryHp(1500);
        createRequest.setArcherAttack(220);
        createRequest.setArcherSiege(180);

        updateRequest = new UpdateGameAccountRequest();
        updateRequest.setAccountName("Updated Account");
        updateRequest.setTroopQuantity(800L); // Test updating troop quantity
        updateRequest.setLvbuStarLevel(BigDecimal.valueOf(4.0));
        updateRequest.setInfantryDefense(130);
        updateRequest.setInfantryHp(1600);
        updateRequest.setArcherAttack(230);
        updateRequest.setArcherSiege(190);

        existingAccount = new GameAccount();
        existingAccount.setId(1L);
        existingAccount.setUserId(100L);
        existingAccount.setServerId(1);
        existingAccount.setAccountName("Existing Account");
        existingAccount.setTroopQuantity(600L);
        existingAccount.setLvbuStarLevel(BigDecimal.valueOf(3.0));
        existingAccount.setInfantryDefense(110);
        existingAccount.setInfantryHp(1400);
        existingAccount.setArcherAttack(210);
        existingAccount.setArcherSiege(170);
    }

    @Test
    void testCreateGameAccountWithTroopQuantity() {
        // Given
        Long userId = 100L;
        
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(userId);
            
            when(userRepository.existsById(userId)).thenReturn(true);
            when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> {
                GameAccount account = invocation.getArgument(0);
                account.setId(1L);
                return account;
            });

            // When
            GameAccount result = gameAccountService.createGameAccount(createRequest);

            // Then
            assertNotNull(result);
            assertEquals(createRequest.getAccountName(), result.getAccountName());
            assertEquals(createRequest.getPowerValue(), result.getPowerValue());
            assertEquals(createRequest.getTroopQuantity(), result.getTroopQuantity()); // Test new field
            assertEquals(createRequest.getLvbuStarLevel(), result.getLvbuStarLevel());
            assertEquals(createRequest.getInfantryDefense(), result.getInfantryDefense());
            assertEquals(createRequest.getInfantryHp(), result.getInfantryHp());
            assertEquals(createRequest.getArcherAttack(), result.getArcherAttack());
            assertEquals(createRequest.getArcherSiege(), result.getArcherSiege());

            verify(gameAccountRepository).save(argThat(account -> 
                account.getTroopQuantity().equals(createRequest.getTroopQuantity())
                && account.getInfantryDefense().equals(createRequest.getInfantryDefense())
                && account.getInfantryHp().equals(createRequest.getInfantryHp())
                && account.getArcherAttack().equals(createRequest.getArcherAttack())
                && account.getArcherSiege().equals(createRequest.getArcherSiege())
            ));
        }
    }

    @Test
    void testUpdateGameAccountWithTroopQuantity() {
        // Given
        Long userId = 100L;
        Long accountId = 1L;
        
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(userId);
            
            when(gameAccountRepository.findById(accountId)).thenReturn(Optional.of(existingAccount));
            when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            GameAccount result = gameAccountService.updateGameAccount(accountId, updateRequest);

            // Then
            assertNotNull(result);
            assertEquals(updateRequest.getAccountName(), result.getAccountName());
            assertEquals(updateRequest.getPowerValue(), result.getPowerValue());
            assertEquals(updateRequest.getTroopQuantity(), result.getTroopQuantity()); // Test updated field
            assertEquals(updateRequest.getLvbuStarLevel(), result.getLvbuStarLevel());
            assertEquals(updateRequest.getInfantryDefense(), result.getInfantryDefense());
            assertEquals(updateRequest.getInfantryHp(), result.getInfantryHp());
            assertEquals(updateRequest.getArcherAttack(), result.getArcherAttack());
            assertEquals(updateRequest.getArcherSiege(), result.getArcherSiege());

            verify(gameAccountRepository).save(argThat(account -> 
                account.getTroopQuantity().equals(updateRequest.getTroopQuantity())
                && account.getInfantryDefense().equals(updateRequest.getInfantryDefense())
                && account.getInfantryHp().equals(updateRequest.getInfantryHp())
                && account.getArcherAttack().equals(updateRequest.getArcherAttack())
                && account.getArcherSiege().equals(updateRequest.getArcherSiege())
            ));
        }
    }

    @Test
    void testCreateGameAccountWithNullTroopQuantity() {
        // Given
        Long userId = 100L;
        createRequest.setTroopQuantity(null); // Test null value
        createRequest.setInfantryDefense(null);
        createRequest.setInfantryHp(null);
        createRequest.setArcherAttack(null);
        createRequest.setArcherSiege(null);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn(userId);
            
            when(userRepository.existsById(userId)).thenReturn(true);
            when(gameAccountRepository.save(any(GameAccount.class))).thenAnswer(invocation -> {
                GameAccount account = invocation.getArgument(0);
                account.setId(1L);
                return account;
            });

            // When
            GameAccount result = gameAccountService.createGameAccount(createRequest);

            // Then
            assertNotNull(result);
            assertNull(result.getTroopQuantity()); // Should be null
            assertNull(result.getInfantryDefense());
            assertNull(result.getInfantryHp());
            assertNull(result.getArcherAttack());
            assertNull(result.getArcherSiege());

            verify(gameAccountRepository).save(argThat(account -> 
                account.getTroopQuantity() == null
                && account.getInfantryDefense() == null
                && account.getInfantryHp() == null
                && account.getArcherAttack() == null
                && account.getArcherSiege() == null
            ));
        }
    }
}
