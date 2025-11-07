package org.veto.api.libs;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RegexpValueValidator implements ConstraintValidator<Regexp, Object> {

    private String regex;  // 用于存储从注解中获取的正则表达式

    private boolean not;

    @Override
    public void initialize(Regexp constraintAnnotation) {
        // 从注解中获取正则表达式
        this.regex = constraintAnnotation.regexp();
        this.not = constraintAnnotation.not();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null || value.toString().isBlank()) return true;
        return this.not != value.toString().matches(regex);
    }
}
