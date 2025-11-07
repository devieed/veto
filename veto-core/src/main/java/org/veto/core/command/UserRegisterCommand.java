package org.veto.core.command;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserRegisterCommand {
    private String username;

    private String password;
    // 推荐码
    private String recommendCode;

    private String nickname;
}
