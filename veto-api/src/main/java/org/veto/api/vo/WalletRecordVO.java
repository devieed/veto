package org.veto.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Setter
@Getter
public class WalletRecordVO {
    private Long id;

    private Boolean isBlockchain;

    private Date createdAt;

    private BigDecimal before;

    private BigDecimal after;

    private BigDecimal fee;

    private BigDecimal amount;

    private String type;
}
