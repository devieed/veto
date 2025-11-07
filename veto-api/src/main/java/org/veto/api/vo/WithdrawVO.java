package org.veto.api.vo;

import lombok.Getter;
import lombok.Setter;
import org.veto.core.rdbms.bean.Withdraw;

import java.math.BigDecimal;

@Setter
@Getter
public class WithdrawVO {
    private Long id;

    private BigDecimal amount;

    private String address;

    private Boolean isBlockchain;

    private BigDecimal fee;

    private String description;

    private Withdraw.STATUS status;
}
