package com.securefindings.audit.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.domain.AuditEvent;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.audit.persistence.FindingAuditRepository;
import com.securefindings.security.OrganizationContext;

@Service
public class AuditService {

    private final FindingAuditRepository auditRepository;
    private final OrganizationContext organizationContext;

    public AuditService(
            FindingAuditRepository auditRepository,
            OrganizationContext organizationContext) {

        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.organizationContext = Objects.requireNonNull(organizationContext);
    }

    @Transactional
    public void register(
            UUID findingId,
            AuditAction action,
            String actor) {

        UUID organizationId = organizationContext.currentOrganizationId();

        AuditEvent event = new AuditEvent(
                findingId,
                organizationId,
                action,
                actor,
                Instant.now());

        FindingAuditEntity entity = new FindingAuditEntity(
                UUID.randomUUID(),
                event);

        auditRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<FindingAuditEntity> findByFindingId(
            UUID findingId) {

        UUID organizationId = organizationContext.currentOrganizationId();

        return auditRepository
                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                        findingId,
                        organizationId);
    }
}