package com.securefindings.organization.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.securefindings.organization.domain.OrganizationStatusAuditAction;

public interface OrganizationStatusAuditRepository
                extends JpaRepository<OrganizationStatusAuditEntity, UUID> {

        Page<OrganizationStatusAuditEntity> findByOrganizationId(
                        UUID organizationId,
                        Pageable pageable);

        @Query("""
                        SELECT audit
                        FROM OrganizationStatusAuditEntity audit
                        WHERE audit.organizationId = :organizationId
                          AND (:action IS NULL OR audit.action = :action)
                          AND (:requestId IS NULL OR audit.requestId = :requestId)
                          AND (:actor IS NULL OR audit.actor = :actor)
                          AND audit.occurredAt >= COALESCE(:occurredFrom, audit.occurredAt)
                          AND audit.occurredAt <= COALESCE(:occurredTo, audit.occurredAt)
                        """)
        Page<OrganizationStatusAuditEntity> findPageByOrganizationIdAndFilters(
                        @Param("organizationId") UUID organizationId,
                        @Param("action") OrganizationStatusAuditAction action,
                        @Param("requestId") String requestId,
                        @Param("actor") String actor,
                        @Param("occurredFrom") Instant occurredFrom,
                        @Param("occurredTo") Instant occurredTo,
                        Pageable pageable);
}