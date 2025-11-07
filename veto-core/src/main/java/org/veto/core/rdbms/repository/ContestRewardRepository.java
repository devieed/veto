package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.ContestReward;

public interface ContestRewardRepository extends JpaRepository<ContestReward, Long>, JpaSpecificationExecutor<ContestReward> {
}
