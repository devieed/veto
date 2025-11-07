package org.veto.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;
import org.veto.api.libs.Regexp;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;

@Setter
@Getter
public class ScoreOddDTO {
    @NotNull
    @Range(min = 1, message = VETO_EXCEPTION_CODE.PARAMS_INVALID)
    private Long id;

    @NotBlank(message = VETO_EXCEPTION_CODE.PARAMS_INVALID)
    @Regexp(message = VETO_EXCEPTION_CODE.PARAMS_INVALID, regexp = "^(?:[1-9]\\d{0,9}|0)(?:\\.\\d{1,2})?$")
    private String total;
}
