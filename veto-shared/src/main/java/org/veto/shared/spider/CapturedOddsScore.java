package org.veto.shared.spider;

import lombok.Getter;
import lombok.Setter;
import org.veto.shared.ODD_SCORE;

import java.math.BigDecimal;

@Setter
@Getter
public class CapturedOddsScore {
    // 是否为半场
    private Boolean isHalf;
    // 比分
    private ODD_SCORE score;
    // 赔付率
    private BigDecimal oddRate;

    private Boolean isActive;
}
