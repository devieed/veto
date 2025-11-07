package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.UserFirstRecharge;

public interface UserFirstRechargeRepository extends JpaRepository<UserFirstRecharge, Long>, JpaSpecificationExecutor<UserFirstRecharge> {
    boolean existsByUserId(Long userId);
}
