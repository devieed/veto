package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.BlockchainRechargeOrder;

public interface BlockchainRechargeOrderRepository extends JpaRepository<BlockchainRechargeOrder, Long>, JpaSpecificationExecutor<BlockchainRechargeOrder> {
    BlockchainRechargeOrder findByTxid(String txid);

    boolean existsByTxidAndStatusIsTrue(String txid);

    Page<BlockchainRechargeOrder> findAllByUserId(Pageable pageable, Long userId);
}
