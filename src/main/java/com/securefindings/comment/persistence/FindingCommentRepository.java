package com.securefindings.comment.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingCommentRepository
        extends JpaRepository<FindingCommentEntity, UUID> {

    Page<FindingCommentEntity> findByFindingIdAndOrganizationIdOrderByCreatedAtAscIdAsc(
            UUID findingId,
            UUID organizationId,
            Pageable pageable);
}