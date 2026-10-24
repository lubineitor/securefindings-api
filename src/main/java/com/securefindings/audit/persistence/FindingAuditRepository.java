package com.securefindings.audit.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.securefindings.audit.domain.AuditAction;

public interface FindingAuditRepository
        extends JpaRepository<FindingAuditEntity, UUID>,
        JpaSpecificationExecutor<FindingAuditEntity> {

    Page<FindingAuditEntity> findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
            UUID findingId,
            UUID organizationId,
            Pageable pageable);

    boolean existsByFindingIdAndOrganizationId(
            UUID findingId,
            UUID organizationId);

    Page<FindingAuditEntity> findByFindingIdAndOrganizationIdAndActionOrderByOccurredAtAsc(
            UUID findingId,
            UUID organizationId,
            AuditAction action,
            Pageable pageable);

    Page<FindingAuditEntity> findByFindingIdAndOrganizationIdAndRequestIdOrderByOccurredAtAsc(
            UUID findingId,
            UUID organizationId,
            String requestId,
            Pageable pageable);

    Page<FindingAuditEntity> findByFindingIdAndOrganizationIdAndActionAndRequestIdOrderByOccurredAtAsc(
            UUID findingId,
            UUID organizationId,
            AuditAction action,
            String requestId,
            Pageable pageable);

    Page<FindingAuditEntity> findByFindingIdAndOrganizationIdAndActorOrderByOccurredAtAsc(
            UUID findingId,
            UUID organizationId,
            String actor,
            Pageable pageable);

    Page<FindingAuditEntity> findByFindingIdAndOrganizationIdAndActionAndActorOrderByOccurredAtAsc(
            UUID findingId,
            UUID organizationId,
            AuditAction action,
            String actor,
            Pageable pageable);

    Page<FindingAuditEntity> findByFindingIdAndOrganizationIdAndRequestIdAndActorOrderByOccurredAtAsc(
            UUID findingId,
            UUID organizationId,
            String requestId,
            String actor,
            Pageable pageable);

    Page<FindingAuditEntity> findByFindingIdAndOrganizationIdAndActionAndRequestIdAndActorOrderByOccurredAtAsc(
            UUID findingId,
            UUID organizationId,
            AuditAction action,
            String requestId,
            String actor,
            Pageable pageable);

    @Query("""
            SELECT event
            FROM FindingAuditEntity event
            WHERE event.findingId = :findingId
              AND event.organizationId = :organizationId
              AND (:action IS NULL OR event.action = :action)
              AND (:requestId IS NULL OR event.requestId = :requestId)
              AND (:actor IS NULL OR event.actor = :actor)
              AND (:occurredFrom IS NULL OR event.occurredAt >= :occurredFrom)
              AND (:occurredTo IS NULL OR event.occurredAt <= :occurredTo)
            """)
    Page<FindingAuditEntity> findPageByFilters(
            @Param("findingId") UUID findingId,
            @Param("organizationId") UUID organizationId,
            @Param("action") AuditAction action,
            @Param("requestId") String requestId,
            @Param("actor") String actor,
            @Param("occurredFrom") Instant occurredFrom,
            @Param("occurredTo") Instant occurredTo,
            Pageable pageable);

    boolean existsByOrganizationId(UUID organizationId);
}
