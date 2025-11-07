package org.veto.api.libs;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 支持正则表达式，给出的表达式如果不能匹配，则抛出异常
 */
@Documented
@Constraint(validatedBy = RegexpValueValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Regexp {
    String regexp();

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String message() default "Invalid regular expression";

    boolean not() default false;
}
