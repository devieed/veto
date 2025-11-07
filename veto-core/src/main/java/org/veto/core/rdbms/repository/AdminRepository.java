package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.Admin;

public interface AdminRepository extends JpaRepository<Admin,Integer>, JpaSpecificationExecutor<Admin> {
    Admin findByUsernameAndPassword(String username, String password);

    Admin findByUsername(String username);

    boolean existsByUsername(String username);
}
