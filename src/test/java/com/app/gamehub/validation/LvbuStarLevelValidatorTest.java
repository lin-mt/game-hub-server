package com.app.gamehub.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LvbuStarLevelValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // 测试用的类
    static class TestClass {
        @ValidLvbuStarLevel
        private BigDecimal lvbuStarLevel;

        public TestClass(BigDecimal lvbuStarLevel) {
            this.lvbuStarLevel = lvbuStarLevel;
        }
    }

    @Test
    void testValidStarLevels() {
        // 测试有效的星级值
        BigDecimal[] validValues = {
            new BigDecimal("0.0"),
            new BigDecimal("0.1"),
            new BigDecimal("0.2"),
            new BigDecimal("0.3"),
            new BigDecimal("0.4"),
            new BigDecimal("1.0"),
            new BigDecimal("1.1"),
            new BigDecimal("1.2"),
            new BigDecimal("1.3"),
            new BigDecimal("1.4"),
            new BigDecimal("2.0"),
            new BigDecimal("2.1"),
            new BigDecimal("2.2"),
            new BigDecimal("2.3"),
            new BigDecimal("2.4"),
            new BigDecimal("3.0"),
            new BigDecimal("3.1"),
            new BigDecimal("3.2"),
            new BigDecimal("3.3"),
            new BigDecimal("3.4"),
            new BigDecimal("4.0"),
            new BigDecimal("4.1"),
            new BigDecimal("4.2"),
            new BigDecimal("4.3"),
            new BigDecimal("4.4"),
            new BigDecimal("5.0")
        };

        for (BigDecimal value : validValues) {
            TestClass testObj = new TestClass(value);
            Set<ConstraintViolation<TestClass>> violations = validator.validate(testObj);
            assertTrue(violations.isEmpty(), "星级 " + value + " 应该是有效的");
        }
    }

    @Test
    void testInvalidStarLevels() {
        // 测试无效的星级值
        BigDecimal[] invalidValues = {
            new BigDecimal("-0.1"), // 负数
            new BigDecimal("0.5"),  // 0.5进1规则，应该是1.0
            new BigDecimal("0.6"),  // 无效小数
            new BigDecimal("0.7"),  // 无效小数
            new BigDecimal("0.8"),  // 无效小数
            new BigDecimal("0.9"),  // 无效小数
            new BigDecimal("1.5"),  // 0.5进1规则，应该是2.0
            new BigDecimal("2.5"),  // 0.5进1规则，应该是3.0
            new BigDecimal("3.5"),  // 0.5进1规则，应该是4.0
            new BigDecimal("4.5"),  // 0.5进1规则，应该是5.0
            new BigDecimal("5.1"),  // 超过最大值
            new BigDecimal("6.0"),  // 超过最大值
            new BigDecimal("0.12"), // 超过一位小数
            new BigDecimal("1.23")  // 超过一位小数
        };

        for (BigDecimal value : invalidValues) {
            TestClass testObj = new TestClass(value);
            Set<ConstraintViolation<TestClass>> violations = validator.validate(testObj);
            assertFalse(violations.isEmpty(), "星级 " + value + " 应该是无效的");
        }
    }

    @Test
    void testNullValue() {
        // 测试null值（应该通过验证，由其他注解处理）
        TestClass testObj = new TestClass(null);
        Set<ConstraintViolation<TestClass>> violations = validator.validate(testObj);
        assertTrue(violations.isEmpty(), "null值应该通过验证");
    }
}
