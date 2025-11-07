package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.RealNameAuthentication;

public interface RealNameAuthenticationRepository extends JpaRepository<RealNameAuthentication, Long>, JpaSpecificationExecutor<RealNameAuthentication> {
    RealNameAuthentication findByUserId(Long userId);
}
