package org.veto.api.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreOddVO {
    private Long id;

    private String score;

    private BigDecimal odd;

    private Boolean isHalf;
}
