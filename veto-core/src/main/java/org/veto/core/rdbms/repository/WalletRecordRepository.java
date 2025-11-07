package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.WalletRecord;

public interface WalletRecordRepository extends JpaRepository<WalletRecord, Long>, JpaSpecificationExecutor<WalletRecord> {
    Page<WalletRecord> findAllByUserIdAndType(Pageable pageable, Long id, WalletRecord.TYPE type);

    Page<WalletRecord> findAllByUserId(Pageable pageable, Long id);
}
