package org.veto.api.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Setter
@Getter
public class ContestOrderVO {
    private Long id;

    private Date createdAt;

    private BigDecimal amount;

    private BigDecimal odds;

    private String status;

    private String score;

    private Boolean isHalf;

    private Boolean ticket;

    private String cnName;

    private String enName;

    private String homeTeamCnName;

    private String homeTeamEnName;

    private String awayTeamCnName;

    private String awayTeamEnName;

    private Date startTime;
}
