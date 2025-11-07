package org.veto.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.veto.api.libs.EnumValue;
import org.veto.api.libs.Regexp;
import org.veto.shared.COIN_TYPE;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;

@Setter
@Getter
public class WithdrawAddDTO {
    // 系统单次启动仅支持一种币种的提取
//    @NotBlank
//    @EnumValue(enumClass = COIN_TYPE.class, message = VETO_EXCEPTION_CODE.WALLET_TYPE_ERROR)
//    private String coinType;

    @NotBlank
    @Regexp(regexp = "^(?:[1-9]\\d{0,9}|0)(?:\\.\\d{1,2})?$", message = VETO_EXCEPTION_CODE.PARAMS_INVALID)
    @Length(max = 20, message =  VETO_EXCEPTION_CODE.PARAMS_INVALID)
    private String amount;

    @NotBlank
    @Length(min = 3, max = 120, message =  VETO_EXCEPTION_CODE.PARAMS_INVALID)
    private String address;

    @Length(max = 200, message =  VETO_EXCEPTION_CODE.PARAMS_INVALID)
    private String description;
}
