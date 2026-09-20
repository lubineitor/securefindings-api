package com.securefindings.audit.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.securefindings.audit.domain.AuditAction;

public interface FindingAuditRepository
                extends JpaRepository<FindingAuditEntity, UUID> {

        List<FindingAuditEntity> findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                        UUID findingId,
                        UUID organizationId);

        Page<FindingAuditEntity> findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                        UUID findingId,
                        UUID organizationId,
                        Pageable pageable);

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
}