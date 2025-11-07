package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.ScoreOdds;
import org.veto.core.rdbms.bean.UserBuyOdds;

import java.util.Date;

public interface UserBuyOddsRepository extends JpaRepository<UserBuyOdds, Long>, JpaSpecificationExecutor<UserBuyOdds> {
    Page<UserBuyOdds> findAllByUserId(Long userId, Pageable pageable);

    Page<UserBuyOdds> findAllByUserIdAndStatus(Long userId, ScoreOdds.STATUS status, Pageable pageable);

    Page<UserBuyOdds> findAllByScoreOddsIdAndStatus(Long scoreId, ScoreOdds.STATUS status, Pageable pageable);

    int countByCreatedAtAfter(Date now);

    Page<UserBuyOdds> findAllByStatusAndCreatedAtLessThanEqual(Pageable pageable, ScoreOdds.STATUS status, Date date);
}
