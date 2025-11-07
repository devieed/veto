package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.RecommendRelation;

public interface RecommendRelationRepository extends JpaRepository<RecommendRelation, Long>, JpaSpecificationExecutor<RecommendRelation> {
    RecommendRelation findByUserId(Long userId);

    Page<RecommendRelation> findAllByTargetUserIdAndStatusIsTrue(Pageable pageable, Long targetUserId);
}

