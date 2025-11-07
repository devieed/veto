package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.RecommendReward;

import java.math.BigDecimal;
import java.util.List;

public interface RecommendRewardRepository extends JpaRepository<RecommendReward, Integer>, JpaSpecificationExecutor<RecommendReward> {
    
    RecommendReward findByLevel(Integer level);

    RecommendReward findByLevelAndStatusIsTrue(Integer level);
    
    boolean existsByLevel(Integer level);
    
    List<RecommendReward> findByOrderByLevelAsc();
    
    List<RecommendReward> findByRateGreaterThan(BigDecimal rate);
    
    List<RecommendReward> findByRateLessThan(BigDecimal rate);
    
    List<RecommendReward> findByRateBetween(BigDecimal minRate, BigDecimal maxRate);
    
    boolean existsByLevelAndIdNot(Integer level, Integer id);

    RecommendReward findFirstByStatusIsTrueOrderByLevelDesc();
}
