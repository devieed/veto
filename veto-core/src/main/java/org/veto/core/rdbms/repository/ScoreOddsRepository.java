package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.ScoreOdds;
import org.veto.shared.ODD_SCORE;

import java.math.BigDecimal;
import java.util.List;

public interface ScoreOddsRepository extends JpaRepository<ScoreOdds, Long>, JpaSpecificationExecutor<ScoreOdds> {

    List<ScoreOdds> findByContestId(Long contestId);

    ScoreOdds findByContestIdAndScoreAndIsHalf(Long contestId, ODD_SCORE score, Boolean isHalf);
}
