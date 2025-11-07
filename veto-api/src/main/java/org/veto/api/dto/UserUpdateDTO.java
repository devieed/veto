package org.veto.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;

@Data
public class UserUpdateDTO {
    @Length(min = 4, max = 20, message = VETO_EXCEPTION_CODE.NICKNAME_INVALID)
    private String nickname;

    @Length(min = 3, max = 120, message = VETO_EXCEPTION_CODE.EMAIL_INVALID)
    private String email;

    @Length(min = 6, max = 72, message = VETO_EXCEPTION_CODE.PASSWORD_INVALID)
    private String currentPassword;

    @Length(min = 6, max = 72, message = VETO_EXCEPTION_CODE.PASSWORD_INVALID)
    private String password;
}
