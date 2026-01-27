package com.app.gamehub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LvbuStarLevelValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLvbuStarLevel {
    String message() default "吕布星级必须在0-5星之间，且遵循0.5进1规则";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
