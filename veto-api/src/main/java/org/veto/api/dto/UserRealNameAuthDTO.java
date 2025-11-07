package org.veto.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;

/**
 * 实名认证
 */
@Setter
@Getter
@ToString
public class UserRealNameAuthDTO {
    @NotBlank
    @Length(min = 2, max = 120, message = VETO_EXCEPTION_CODE.PARAMS_INVALID)
    private String phone;

    @NotBlank
    @Length(min = 2, max = 60, message = VETO_EXCEPTION_CODE.PARAMS_INVALID)
    private String realName;

    @NotBlank
    @Length(min = 2, max = 120, message = VETO_EXCEPTION_CODE.PARAMS_INVALID)
    private String idNumber;
}
