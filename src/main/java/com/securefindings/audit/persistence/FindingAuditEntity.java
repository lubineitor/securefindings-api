package com.securefindings.audit.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.domain.AuditEvent;

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

    @Column(name = "finding_id", nullable = false, updatable = false)
    private UUID findingId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

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
            AuditEvent event) {

        this(
                id,
                event.findingId(),
                event.organizationId(),
                event.action(),
                event.actor(),
                event.occurredAt());
    }

    public FindingAuditEntity(
            UUID id,
            UUID findingId,
            UUID organizationId,
            AuditAction action,
            String actor,
            Instant occurredAt) {

        this.id = Objects.requireNonNull(id);
        this.findingId = Objects.requireNonNull(findingId);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.action = Objects.requireNonNull(action);
        this.actor = Objects.requireNonNull(actor);
        this.occurredAt = Objects.requireNonNull(occurredAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getFindingId() {
        return findingId;
    }

    @JsonIgnore
    public UUID getOrganizationId() {
        return organizationId;
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