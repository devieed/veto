package org.veto.core.command;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserLoginCommand {
    private String username;

    private String password;

    private String captcha;

    private String captchaId;
}
