package com.securefindings.audit.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.domain.AuditEvent;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.audit.persistence.FindingAuditRepository;

@Service
public class AuditService {

    private final FindingAuditRepository auditRepository;

    public AuditService(FindingAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void register(
            UUID findingId,
            AuditAction action,
            String actor) {

        AuditEvent event = new AuditEvent(
                findingId,
                action,
                actor,
                Instant.now());

        FindingAuditEntity entity = new FindingAuditEntity(
                UUID.randomUUID(),
                event.findingId(),
                event.action(),
                event.actor(),
                event.occurredAt());

        auditRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<FindingAuditEntity> findByFindingId(UUID findingId) {
        return auditRepository
                .findByFindingIdOrderByOccurredAtAsc(findingId);
    }
}