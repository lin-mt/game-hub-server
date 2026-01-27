package com.app.gamehub.model;

import com.app.gamehub.entity.GameAccount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WarTacticTest {

    private List<GameAccount> createTestAccounts(int count) {
        List<GameAccount> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GameAccount account = new GameAccount();
            account.setId((long) (i + 1));
            account.setAccountName("账号" + (i + 1));
            // 设置伤害加成，从大到小排列
            account.setDamageBonus(BigDecimal.valueOf(100 - i));
            accounts.add(account);
        }
        return accounts;
    }

    @Test
    void testGuanDuOne_WithFullAccounts() {
        // Given
        List<GameAccount> accounts = createTestAccounts(30);
        
        // When
        List<TacticalArrangement> arrangements = WarTactic.TACTIC_ONE.arrangement(accounts);

        // Then
        assertEquals(6, arrangements.size());

        // 验证每个小组都有成员
        for (TacticalArrangement arrangement : arrangements) {
            assertFalse(arrangement.getWarArrangements().isEmpty());
            assertNotNull(arrangement.getWarGroup().getGroupName());
            assertNotNull(arrangement.getWarGroup().getGroupTask());
            // 组名应至少包含组长的账号名（WarTactic.md 中的格式如：兵器坊（车头：账号名））
            String leaderName = arrangement.getWarArrangements().get(0).getAccount().getAccountName();
            assertTrue(arrangement.getWarGroup().getGroupName().contains(leaderName));
        }
        
        // 验证总人数
        int totalMembers = arrangements.stream()
                .mapToInt(arr -> arr.getWarArrangements().size())
                .sum();
        assertEquals(30, totalMembers);
    }

    @Test
    void testGuanDuOne_WithFewerAccounts() {
        // Given
        List<GameAccount> accounts = createTestAccounts(10);
        
        // When
        List<TacticalArrangement> arrangements = WarTactic.TACTIC_ONE.arrangement(accounts);

        // Then
        assertEquals(6, arrangements.size());

        // 验证总人数
        int totalMembers = arrangements.stream()
                .mapToInt(arr -> arr.getWarArrangements().size())
                .sum();
        assertEquals(10, totalMembers);
        
        // 验证小组命名
        for (TacticalArrangement arrangement : arrangements) {
            if (!arrangement.getWarArrangements().isEmpty()) {
                String groupName = arrangement.getWarGroup().getGroupName();
                // 验证组长是该组中加成最高的（第一个），组名应包含组长名称
                String leaderName = arrangement.getWarArrangements().get(0).getAccount().getAccountName();
                assertTrue(groupName.contains(leaderName));
            }
        }
    }

    @Test
    void testGuanDuTwo_WithFullAccounts() {
        // Given
        List<GameAccount> accounts = createTestAccounts(30);
        
        // When
        List<TacticalArrangement> arrangements = WarTactic.TACTIC_TWO.arrangement(accounts);

        // Then
        assertEquals(6, arrangements.size());
        
        // 验证每个小组都有成员
        for (TacticalArrangement arrangement : arrangements) {
            assertFalse(arrangement.getWarArrangements().isEmpty());
            assertNotNull(arrangement.getWarGroup().getGroupName());
            assertNotNull(arrangement.getWarGroup().getGroupTask());
            String leaderName = arrangement.getWarArrangements().get(0).getAccount().getAccountName();
            assertTrue(arrangement.getWarGroup().getGroupName().contains(leaderName));
        }
        
        // 验证总人数
        int totalMembers = arrangements.stream()
                .mapToInt(arr -> arr.getWarArrangements().size())
                .sum();
        assertEquals(30, totalMembers);
    }

    @Test
    void testGuanDuTwo_WithFewerAccounts() {
        // Given
        List<GameAccount> accounts = createTestAccounts(15);
        
        // When
        List<TacticalArrangement> arrangements = WarTactic.TACTIC_TWO.arrangement(accounts);

        // Then
        assertEquals(6, arrangements.size());
        
        // 验证总人数
        int totalMembers = arrangements.stream()
                .mapToInt(arr -> arr.getWarArrangements().size())
                .sum();
        assertEquals(15, totalMembers);
    }

    @Test
    void testGuanDuThree_WithFewerAccounts() {
        // Given
        List<GameAccount> accounts = createTestAccounts(20);
        
        // When
        List<TacticalArrangement> arrangements = WarTactic.TACTIC_TWO.arrangement(accounts);

        // Then
        assertEquals(6, arrangements.size());
        
        // 验证总人数
        int totalMembers = arrangements.stream()
                .mapToInt(arr -> arr.getWarArrangements().size())
                .sum();
        assertEquals(20, totalMembers);
    }

    @Test
    void testGuanDuFive_WithFewerAccounts() {
        // Given
        List<GameAccount> accounts = createTestAccounts(25);
        
        // When
        List<TacticalArrangement> arrangements = WarTactic.TACTIC_TWO.arrangement(accounts);

        // Then
        assertEquals(6, arrangements.size());
        
        // 验证总人数
        int totalMembers = arrangements.stream()
                .mapToInt(arr -> arr.getWarArrangements().size())
                .sum();
        assertEquals(25, totalMembers);
    }

    @Test
    void testEmptyAccountsList() {
        // Given
        List<GameAccount> accounts = new ArrayList<>();
        
        // When
        List<TacticalArrangement> arrangements = WarTactic.TACTIC_ONE.arrangement(accounts);

        // Then
        assertTrue(arrangements.isEmpty());
    }

    @Test
    void testSingleAccount() {
        // Given
        List<GameAccount> accounts = createTestAccounts(1);
        
        // When
        List<TacticalArrangement> arrangements = WarTactic.TACTIC_ONE.arrangement(accounts);

        // Then
        assertEquals(6, arrangements.size());

        // 只有第一个小组有成员
        assertEquals(1, arrangements.get(0).getWarArrangements().size());
        assertEquals(0, arrangements.get(1).getWarArrangements().size());
        assertEquals(0, arrangements.get(2).getWarArrangements().size());
        assertEquals(0, arrangements.get(3).getWarArrangements().size());
        assertEquals(0, arrangements.get(4).getWarArrangements().size());
        assertEquals(0, arrangements.get(5).getWarArrangements().size());

        // 验证小组命名
        String actualName = arrangements.get(0).getWarGroup().getGroupName();
        assertTrue(actualName.contains("账号1"));
    }

    @Test
    void testGroupNamingFormat() {
        // Given
        List<GameAccount> accounts = createTestAccounts(5);
        accounts.get(0).setAccountName("小水怪");
        
        // When
        List<TacticalArrangement> arrangements = WarTactic.TACTIC_ONE.arrangement(accounts);

        // Then
        // 第一个小组的组长是小水怪（加成最高）
        assertTrue(arrangements.get(0).getWarGroup().getGroupName().contains("小水怪"));
    }
}
