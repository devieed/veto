package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.Announcement;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long>, JpaSpecificationExecutor<Announcement> {
    Page<Announcement> findAllByStatusIsTrue(Pageable pageable);

    Page<Announcement> findAllByStatusIsTrueAndType(Pageable pageable, Announcement.TYPE type);
}
