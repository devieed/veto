package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.Team;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Integer>, JpaSpecificationExecutor<Team> {
    
    Team findByCnName(String name);
    
    boolean existsByCnName(String name);
    List<Team> findByStatus(Boolean status);
    
    List<Team> findByStatusOrderByCnNameAsc(Boolean status);
    
    List<Team> findByCnNameContainingIgnoreCase(String name);
    
    boolean existsByCnNameAndIdNot(String name, Integer id);

    Page<Team> findAllByIdIn(List<Integer> ids, Pageable pageable);

    Page<Team> findAllByCnNameContainingIgnoreCase(String name, Pageable pageable);

}
