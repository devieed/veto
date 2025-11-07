package org.veto.api.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class UserVO {
    private String id;

    private String nickname;

    private String createdAt;

    private String recommendCode;

    private String avatar;

    private Integer level;
}
