package com.app.gamehub.util;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LvbuStarLevelUtilTest {

    @Test
    void testIsValidStarLevel() {
        // 测试有效值
        assertTrue(LvbuStarLevelUtil.isValidStarLevel(new BigDecimal("0.0")));
        assertTrue(LvbuStarLevelUtil.isValidStarLevel(new BigDecimal("0.4")));
        assertTrue(LvbuStarLevelUtil.isValidStarLevel(new BigDecimal("1.0")));
        assertTrue(LvbuStarLevelUtil.isValidStarLevel(new BigDecimal("3.2")));
        assertTrue(LvbuStarLevelUtil.isValidStarLevel(new BigDecimal("5.0")));

        // 测试无效值
        assertFalse(LvbuStarLevelUtil.isValidStarLevel(new BigDecimal("0.5")));
        assertFalse(LvbuStarLevelUtil.isValidStarLevel(new BigDecimal("1.5")));
        assertFalse(LvbuStarLevelUtil.isValidStarLevel(new BigDecimal("5.1")));
        assertFalse(LvbuStarLevelUtil.isValidStarLevel(new BigDecimal("-0.1")));
        assertFalse(LvbuStarLevelUtil.isValidStarLevel(null));
    }

    @Test
    void testFormatStarLevel() {
        assertEquals("0.0星", LvbuStarLevelUtil.formatStarLevel(new BigDecimal("0.0")));
        assertEquals("3.2星", LvbuStarLevelUtil.formatStarLevel(new BigDecimal("3.2")));
        assertEquals("5.0星", LvbuStarLevelUtil.formatStarLevel(new BigDecimal("5.0")));
        assertEquals("0.0星", LvbuStarLevelUtil.formatStarLevel(null));
    }

    @Test
    void testAdjustStarLevel() {
        // 测试0.5进1规则
        assertEquals(new BigDecimal("1.0"), LvbuStarLevelUtil.adjustStarLevel(new BigDecimal("0.5")));
        assertEquals(new BigDecimal("2.0"), LvbuStarLevelUtil.adjustStarLevel(new BigDecimal("1.5")));
        assertEquals(new BigDecimal("3.0"), LvbuStarLevelUtil.adjustStarLevel(new BigDecimal("2.5")));
        assertEquals(new BigDecimal("4.0"), LvbuStarLevelUtil.adjustStarLevel(new BigDecimal("3.5")));
        assertEquals(new BigDecimal("5.0"), LvbuStarLevelUtil.adjustStarLevel(new BigDecimal("4.5")));

        // 测试小数部分保留
        assertEquals(new BigDecimal("0.1"), LvbuStarLevelUtil.adjustStarLevel(new BigDecimal("0.1")));
        assertEquals(new BigDecimal("0.4"), LvbuStarLevelUtil.adjustStarLevel(new BigDecimal("0.4")));
        assertEquals(new BigDecimal("1.2"), LvbuStarLevelUtil.adjustStarLevel(new BigDecimal("1.2")));

        // 测试null值
        assertEquals(BigDecimal.ZERO, LvbuStarLevelUtil.adjustStarLevel(null));
    }

    @Test
    void testParseStarLevel() {
        assertEquals(new BigDecimal("3.2"), LvbuStarLevelUtil.parseStarLevel("3.2"));
        assertEquals(new BigDecimal("3.2"), LvbuStarLevelUtil.parseStarLevel("3.2星"));
        assertEquals(new BigDecimal("0.0"), LvbuStarLevelUtil.parseStarLevel("0.0"));
        assertEquals(BigDecimal.ZERO, LvbuStarLevelUtil.parseStarLevel(""));
        assertEquals(BigDecimal.ZERO, LvbuStarLevelUtil.parseStarLevel(null));

        // 测试无效值抛出异常
        assertThrows(IllegalArgumentException.class, () -> 
            LvbuStarLevelUtil.parseStarLevel("0.5"));
        assertThrows(IllegalArgumentException.class, () -> 
            LvbuStarLevelUtil.parseStarLevel("invalid"));
    }

    @Test
    void testGetAllValidStarLevels() {
        List<BigDecimal> validLevels = LvbuStarLevelUtil.getAllValidStarLevels();
        
        // 验证数量：每个整数星级有5个值（x.0, x.1, x.2, x.3, x.4），共6个整数星级（0-5）
        assertEquals(26, validLevels.size());
        
        // 验证包含关键值
        assertTrue(validLevels.contains(new BigDecimal("0.0")));
        assertTrue(validLevels.contains(new BigDecimal("0.4")));
        assertTrue(validLevels.contains(new BigDecimal("1.0")));
        assertTrue(validLevels.contains(new BigDecimal("5.0")));
        
        // 验证不包含无效值
        assertFalse(validLevels.contains(new BigDecimal("0.5")));
        assertFalse(validLevels.contains(new BigDecimal("1.5")));
    }
}
