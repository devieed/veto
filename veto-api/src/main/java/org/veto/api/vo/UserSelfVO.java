package org.veto.api.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserSelfVO {
    private String id;

    private String createdAt;

    private String avatar;

    private String username;

    private String lastLoginAt;

    private String recommendCode;

    private Integer recommendUserCount;

    private String email;

    private String phone;

    private Boolean isVip;
}
