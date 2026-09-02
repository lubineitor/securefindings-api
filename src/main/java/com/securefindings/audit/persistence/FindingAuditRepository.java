package com.securefindings.audit.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingAuditRepository
        extends JpaRepository<FindingAuditEntity, UUID> {

    List<FindingAuditEntity> findByFindingIdOrderByOccurredAtAsc(
            UUID findingId);
}