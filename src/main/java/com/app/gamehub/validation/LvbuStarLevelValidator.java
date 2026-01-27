package com.app.gamehub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class LvbuStarLevelValidator implements ConstraintValidator<ValidLvbuStarLevel, BigDecimal> {

    @Override
    public void initialize(ValidLvbuStarLevel constraintAnnotation) {
        // 初始化方法，可以为空
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null值由@NotNull等其他注解处理
        }

        // 检查范围：0-5星
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("5.0")) > 0) {
            return false;
        }

        // 检查精度：只允许一位小数
        if (value.scale() > 1) {
            return false;
        }

        // 检查0.5进1规则：小数部分只能是0.0、0.1、0.2、0.3、0.4
        BigDecimal fractionalPart = value.remainder(BigDecimal.ONE);
        
        // 允许的小数部分
        BigDecimal[] allowedFractions = {
            new BigDecimal("0.0"),
            new BigDecimal("0.1"), 
            new BigDecimal("0.2"),
            new BigDecimal("0.3"),
            new BigDecimal("0.4")
        };

        for (BigDecimal allowedFraction : allowedFractions) {
            if (fractionalPart.compareTo(allowedFraction) == 0) {
                return true;
            }
        }

        return false;
    }
}
