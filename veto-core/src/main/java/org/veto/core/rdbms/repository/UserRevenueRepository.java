package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.UserRevenue;
import org.veto.shared.COIN_TYPE;

public interface UserRevenueRepository extends JpaRepository<UserRevenue, Long>, JpaSpecificationExecutor<UserRevenue> {
    UserRevenue findByUserIdAndTypeAndCoinType(Long userId, UserRevenue.TYPE type, COIN_TYPE coinType);
}
