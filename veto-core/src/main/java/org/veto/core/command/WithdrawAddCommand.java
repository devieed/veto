package org.veto.core.command;

import lombok.Data;
import org.veto.shared.COIN_TYPE;

import java.math.BigDecimal;

@Data
public class WithdrawAddCommand {
    private BigDecimal amount;

    private String address;

    private String description;
}
