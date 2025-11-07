package org.veto.core.command;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserUpdateCommand {
    private String nickname;

    private String password;

    private String currentPassword;

    private String email;

    private Boolean status;

    private String phone;

    private String realName;
}
