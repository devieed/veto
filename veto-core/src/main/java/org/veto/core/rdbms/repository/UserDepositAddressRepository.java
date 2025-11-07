package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.UserDepositAddress;
import org.veto.shared.COIN_TYPE;

public interface UserDepositAddressRepository extends JpaRepository<UserDepositAddress, Long>, JpaSpecificationExecutor<UserDepositAddress> {
    UserDepositAddress findByAddressAndStatusIsTrue(String address);

    UserDepositAddress findByUserIdAndCoinTypeAndStatusIsTrue(long userId, COIN_TYPE coinType);
}
