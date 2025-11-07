package org.veto.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class UserWalletVO {
    private BigDecimal balance;

    private String coinType;
    // 流水
    private BigDecimal flowLimit;
    // 累计收益
    private BigDecimal reward;

    private BigDecimal totalBets;
}
