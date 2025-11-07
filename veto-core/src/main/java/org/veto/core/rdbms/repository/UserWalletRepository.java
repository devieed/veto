package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.shared.COIN_TYPE;
import org.veto.core.rdbms.bean.UserWallet;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long>, JpaSpecificationExecutor<UserWallet> {
    UserWallet findByUserIdAndCoinType(Long id, COIN_TYPE coinType);

    Page<UserWallet> findAllByUserIdAndCoinType(Long id, COIN_TYPE coinType, Pageable pageable);

    Page<UserWallet> findAllByUserId(Long userId, Pageable pageable);

    Page<UserWallet> findAllByCoinType(COIN_TYPE coinType, Pageable pageable);
}
