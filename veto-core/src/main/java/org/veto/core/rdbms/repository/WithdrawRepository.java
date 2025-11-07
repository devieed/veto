package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.Withdraw;

public interface WithdrawRepository extends JpaRepository<Withdraw, Long>, JpaSpecificationExecutor<Withdraw> {
    Page<Withdraw> findAllByUserIdAndStatus(Long userId, Withdraw.STATUS status, Pageable pageable);
    Page<Withdraw> findAllByUserId(Long userId, Pageable pageable);
    Page<Withdraw> findAllByStatus(Withdraw.STATUS status, Pageable pageable);
}
