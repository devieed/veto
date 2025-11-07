package org.veto.api.vo;

import lombok.Data;
import org.veto.core.rdbms.bean.ScoreOdds;

import java.util.Date;
import java.util.List;

@Data
public class ContestVO {
    private Long id;

    private String cnName;

    private String enName;

    private String homeTeamCnName;

    private String homeTeamEnName;

    private Integer homeTeamId;

    private String homeTeamIco;

    private Integer awayTeamId;

    private String awayTeamCnName;

    private String awayTeamEnName;

    private String awayTeamIco;

    private Date startTime;

    private Date endTime;

    private List<ScoreOddVO> half;

    private List<ScoreOddVO> full;

    private List<ScoreOddVO> hots;
}
