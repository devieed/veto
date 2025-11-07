package org.veto.api.libs;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EnumValueValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValue {
    Class<? extends Enum<?>> enumClass() default DefaultEnum.class;  // 指定枚举类
    String[] acceptedValues() default {}; // 可选，指定接受的枚举值列表
    boolean ignoreCase() default true; // 是否忽略大小写
    String message() default "值必须是指定枚举中的一个";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    enum DefaultEnum{}
}
