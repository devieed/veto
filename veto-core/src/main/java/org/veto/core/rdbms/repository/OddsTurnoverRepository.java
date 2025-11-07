package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.veto.core.rdbms.bean.OddsTurnover;

import java.math.BigDecimal;
import java.util.List;

public interface OddsTurnoverRepository extends JpaRepository<OddsTurnover, Long>, JpaSpecificationExecutor<OddsTurnover> {
    
    List<OddsTurnover> findByContestId(Long contestId);
    
    OddsTurnover findByOddsId(Long oddsId);
    
    OddsTurnover findByContestIdAndOddsId(Long contestId, Long oddsId);
    
    boolean existsByContestIdAndOddsId(Long contestId, Long oddsId);
    
    List<OddsTurnover> findByContestIdOrderByTurnoverDesc(Long contestId);
    
    List<OddsTurnover> findByTurnoverGreaterThan(BigDecimal turnover);
    
    List<OddsTurnover> findByTurnoverBetween(BigDecimal minTurnover, BigDecimal maxTurnover);

    @Query("SELECT SUM(ot.turnover) FROM OddsTurnover ot WHERE ot.contestId = ?1")
    BigDecimal sumTurnoverByContestId(Long contestId);
    
    @Query("SELECT SUM(ot.turnover) FROM OddsTurnover ot WHERE ot.oddsId = ?1")
    BigDecimal sumTurnoverByOddsId(Long oddsId);
}
