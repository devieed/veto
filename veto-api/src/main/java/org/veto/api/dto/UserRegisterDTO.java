package org.veto.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.veto.api.libs.Regexp;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;

@Data
public class UserRegisterDTO {
    @NotBlank
    @Length(min = 6, max = 20, message = VETO_EXCEPTION_CODE.REGISTER_USERNAME_INVALID)
    private String username;

    @Length(min = 4, message = VETO_EXCEPTION_CODE.REGISTER_RECOMMEND_CODE_INVALID)
    private String recommendCode;

    @NotBlank
    @Length(min = 6, max = 72, message = VETO_EXCEPTION_CODE.REGISTER_PASSWORD_INVALID)
    private String password;

    @Length(min = 1, max = 8, message = VETO_EXCEPTION_CODE.CAPTCHA_INVALID)
    private String captcha;

    @Regexp(regexp = "[a-zA-Z0-9]+", not = false, message = VETO_EXCEPTION_CODE.CAPTCHA_INVALID)
    @Length(min = 1, max = 120, message = VETO_EXCEPTION_CODE.CAPTCHA_INVALID)
    private String captchaId;
}
