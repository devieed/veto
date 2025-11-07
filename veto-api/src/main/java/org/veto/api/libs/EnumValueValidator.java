package org.veto.api.libs;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.Set;

public class EnumValueValidator implements ConstraintValidator<EnumValue, Object> {

    private Set<String> validValues;
    private boolean ignoreCase;

    @Override
    public void initialize(EnumValue annotation) {
        validValues = new HashSet<>();
        ignoreCase = annotation.ignoreCase();

        // 1. 枚举值添加
        Class<? extends Enum<?>> enumClass = annotation.enumClass();
        if (!enumClass.equals(EnumValue.DefaultEnum.class)) {
            for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                validValues.add(processValue(enumConstant.name()));
            }
        }

        // 2. 添加 acceptedValues
        for (String val : annotation.acceptedValues()) {
            validValues.add(processValue(val));
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true; // 允许空值

        if (value instanceof String str) {
            return containsIgnoreCase(str);
        }
        if (value instanceof Enum<?> e) {
            return containsIgnoreCase(e.name());
        }
        // 其他类型一律不通过
        return false;
    }

    private String processValue(String val) {
        return ignoreCase ? val.toLowerCase() : val;
    }

    private boolean containsIgnoreCase(String input) {
        if (ignoreCase) {
            return validValues.contains(input.toLowerCase());
        } else {
            return validValues.contains(input);
        }
    }
}