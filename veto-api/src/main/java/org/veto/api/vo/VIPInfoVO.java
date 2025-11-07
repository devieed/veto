package org.veto.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class VIPInfoVO {
    private String name;

    private String icon;

    private BigDecimal totalSpending;

    private Integer membershipLevel;

    private BigDecimal withdrawalFee;

    private BigDecimal rechargeFee;

    private BigDecimal betHandlingFee;

    private Boolean currentLevel;

    private Integer withdrawalRechargeRate;
}
