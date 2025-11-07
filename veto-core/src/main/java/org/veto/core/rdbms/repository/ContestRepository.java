package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.Contest;

import java.util.Date;

public interface ContestRepository extends JpaRepository<Contest, Long>, JpaSpecificationExecutor<Contest> {
    Page<Contest> findAllByStatusAndStartTimeAfter(Pageable pageable, Contest.STATUS status, Date startTime);

    Page<Contest> findAllByCnNameContainsOrEnNameContainsAndStatusAndStartTimeAfter(Pageable pageable, String name, String enName, Contest.STATUS status, Date startTime);

    Page<Contest> findAllByCnNameOrEnNameAndStatusAndStartTimeAfter(Pageable pageable, String name, String enName, Contest.STATUS status, Date startTime);

    boolean existsByCnNameAndStartTimeAfter(String name, Date startTime);

    boolean existsByCnNameAndStartTime(String cnname, Date startTime);

    Contest findByCnNameAndStartTime(String cnname, Date startTime);

    Contest findByCnNameAndEnNameAndHomeTeamIdAndAwayTeamIdAndStartTime(String cnname, String enname, Integer homeTeamId, Integer awayTeamId, Date startTime);
}
