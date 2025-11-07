package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.WithdrawRecord;

public interface WithdrawRecordRepository extends JpaRepository<WithdrawRecord, Long>, JpaSpecificationExecutor<WithdrawRecord> {
}
