package com.securefindings.audit.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.domain.AuditEvent;
import com.securefindings.finding.domain.FindingStatus;

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

    @Column(name = "request_id", length = 64, updatable = false)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20, updatable = false)
    private FindingStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20, updatable = false)
    private FindingStatus newStatus;

    protected FindingAuditEntity() {
    }

    public FindingAuditEntity(UUID id, AuditEvent event) {
        this(
                id,
                event.findingId(),
                event.organizationId(),
                event.action(),
                event.actor(),
                event.occurredAt(),
                event.requestId(),
                event.previousStatus(),
                event.newStatus());
    }

    public FindingAuditEntity(
            UUID id,
            UUID findingId,
            UUID organizationId,
            AuditAction action,
            String actor,
            Instant occurredAt,
            String requestId,
            FindingStatus previousStatus,
            FindingStatus newStatus) {
        this.id = Objects.requireNonNull(id);
        this.findingId = Objects.requireNonNull(findingId);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.action = Objects.requireNonNull(action);
        this.actor = Objects.requireNonNull(actor);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.requestId = requestId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public FindingAuditEntity(
            UUID id,
            UUID findingId,
            UUID organizationId,
            AuditAction action,
            String actor,
            Instant occurredAt,
            String requestId) {
        this(
                id,
                findingId,
                organizationId,
                action,
                actor,
                occurredAt,
                requestId,
                null,
                null);
    }

    public FindingAuditEntity(
            UUID id,
            UUID findingId,
            UUID organizationId,
            AuditAction action,
            String actor,
            Instant occurredAt) {
        this(
                id,
                findingId,
                organizationId,
                action,
                actor,
                occurredAt,
                null,
                null,
                null);
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

    public String getRequestId() {
        return requestId;
    }

    public FindingStatus getPreviousStatus() {
        return previousStatus;
    }

    public FindingStatus getNewStatus() {
        return newStatus;
    }
}