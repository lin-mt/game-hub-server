package com.app.gamehub.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * 吕布星级工具类
 * 处理吕布星级的验证、转换和格式化
 */
public class LvbuStarLevelUtil {

    // 允许的小数部分
    private static final List<BigDecimal> ALLOWED_FRACTIONS = Arrays.asList(
        new BigDecimal("0.0"),
        new BigDecimal("0.1"),
        new BigDecimal("0.2"),
        new BigDecimal("0.3"),
        new BigDecimal("0.4")
    );

    // 最小星级
    public static final BigDecimal MIN_STAR_LEVEL = BigDecimal.ZERO;
    
    // 最大星级
    public static final BigDecimal MAX_STAR_LEVEL = new BigDecimal("5.0");

    /**
     * 验证吕布星级是否有效
     * @param starLevel 星级值
     * @return 是否有效
     */
    public static boolean isValidStarLevel(BigDecimal starLevel) {
        if (starLevel == null) {
            return false;
        }

        // 检查范围：0-5星
        if (starLevel.compareTo(MIN_STAR_LEVEL) < 0 || starLevel.compareTo(MAX_STAR_LEVEL) > 0) {
            return false;
        }

        // 检查精度：只允许一位小数
        if (starLevel.scale() > 1) {
            return false;
        }

        // 检查0.5进1规则：小数部分只能是0.0、0.1、0.2、0.3、0.4
        BigDecimal fractionalPart = starLevel.remainder(BigDecimal.ONE);
        return ALLOWED_FRACTIONS.contains(fractionalPart);
    }

    /**
     * 格式化星级显示
     * @param starLevel 星级值
     * @return 格式化后的字符串，如"3.2星"
     */
    public static String formatStarLevel(BigDecimal starLevel) {
        if (starLevel == null) {
            return "0.0星";
        }
        return starLevel.setScale(1, RoundingMode.HALF_UP) + "星";
    }

    /**
     * 根据0.5进1规则调整星级
     * 例如：0.5 -> 1.0, 1.5 -> 2.0, 2.5 -> 3.0
     * @param rawStarLevel 原始星级
     * @return 调整后的星级
     */
    public static BigDecimal adjustStarLevel(BigDecimal rawStarLevel) {
        if (rawStarLevel == null) {
            return BigDecimal.ZERO;
        }

        // 获取整数部分和小数部分
        BigDecimal integerPart = rawStarLevel.setScale(0, RoundingMode.DOWN);
        BigDecimal fractionalPart = rawStarLevel.remainder(BigDecimal.ONE);

        // 如果小数部分 >= 0.5，则进位到下一个整数
        if (fractionalPart.compareTo(new BigDecimal("0.5")) >= 0) {
            return integerPart.add(BigDecimal.ONE).setScale(1, RoundingMode.HALF_UP);
        } else {
            // 否则保留小数部分，但只保留到0.4
            if (fractionalPart.compareTo(new BigDecimal("0.4")) > 0) {
                fractionalPart = new BigDecimal("0.4");
            }
            return integerPart.add(fractionalPart).setScale(1, RoundingMode.HALF_UP);
        }
    }

    /**
     * 获取所有有效的星级值列表（用于测试或下拉选择）
     * @return 有效星级值列表
     */
    public static List<BigDecimal> getAllValidStarLevels() {
        return Arrays.asList(
            new BigDecimal("0.0"), new BigDecimal("0.1"), new BigDecimal("0.2"), new BigDecimal("0.3"), new BigDecimal("0.4"),
            new BigDecimal("1.0"), new BigDecimal("1.1"), new BigDecimal("1.2"), new BigDecimal("1.3"), new BigDecimal("1.4"),
            new BigDecimal("2.0"), new BigDecimal("2.1"), new BigDecimal("2.2"), new BigDecimal("2.3"), new BigDecimal("2.4"),
            new BigDecimal("3.0"), new BigDecimal("3.1"), new BigDecimal("3.2"), new BigDecimal("3.3"), new BigDecimal("3.4"),
            new BigDecimal("4.0"), new BigDecimal("4.1"), new BigDecimal("4.2"), new BigDecimal("4.3"), new BigDecimal("4.4"),
            new BigDecimal("5.0")
        );
    }

    /**
     * 解析字符串为星级值
     * @param starLevelStr 星级字符串，如"3.2"或"3.2星"
     * @return 星级值
     * @throws IllegalArgumentException 如果格式不正确
     */
    public static BigDecimal parseStarLevel(String starLevelStr) {
        if (starLevelStr == null || starLevelStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 移除"星"字符
        String cleanStr = starLevelStr.replace("星", "").trim();
        
        try {
            BigDecimal starLevel = new BigDecimal(cleanStr);
            if (isValidStarLevel(starLevel)) {
                return starLevel;
            } else {
                throw new IllegalArgumentException("无效的星级值: " + starLevelStr);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法解析星级值: " + starLevelStr, e);
        }
    }
}
