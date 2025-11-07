package org.veto.shared.spider;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
public class CapturedContest {
    private String cnName;

    private String enName;

    private CapturedImage icon;

    private Date startTime;

    private Boolean isFinished;

    private Integer homeTeamFullScore;

    private Integer homeHalfScore;

    private Integer awayTeamFullScore;

    private Integer awayTeamHalfScore;

    private CapturedTeam homeTeam;

    private CapturedTeam awayTeam;

    private Boolean exist;

    private List<CapturedOddsScore> capturedOddsScores;
}
