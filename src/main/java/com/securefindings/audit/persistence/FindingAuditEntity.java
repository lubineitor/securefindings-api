package com.securefindings.audit.persistence;

import java.time.Instant;
import java.util.UUID;

import com.securefindings.audit.domain.AuditAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "finding_audit")
public class FindingAuditEntity {

    @Id
    private UUID id;

    @Column(name = "finding_id", nullable = false)
    private UUID findingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AuditAction action;

    @Column(name = "actor", nullable = false, length = 255)
    private String actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected FindingAuditEntity() {
    }

    public FindingAuditEntity(
            UUID id,
            UUID findingId,
            AuditAction action,
            String actor,
            Instant occurredAt) {

        this.id = id;
        this.findingId = findingId;
        this.action = action;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFindingId() {
        return findingId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getActor() {
        return actor;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}