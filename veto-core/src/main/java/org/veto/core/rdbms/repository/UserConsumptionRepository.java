package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.veto.core.rdbms.bean.UserConsumption;
import org.veto.core.rdbms.bean.WALLET_BEHAVIOR_TYPE;
import org.veto.shared.COIN_TYPE;

import java.math.BigDecimal;

public interface UserConsumptionRepository extends JpaRepository<UserConsumption, Long>, JpaSpecificationExecutor<UserConsumption> {
    UserConsumption findByUserIdAndCoinTypeAndWalletBehaviorType(Long userId, COIN_TYPE coinType, WALLET_BEHAVIOR_TYPE walletBehaviorType);
}
