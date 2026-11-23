package com.securefindings.organization.persistence;

import java.time.Instant;
import java.util.UUID;

import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.domain.OrganizationStatusAuditEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organization_status_audit")
public class OrganizationStatusAuditEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20, updatable = false)
    private OrganizationStatusAuditAction action;

    @Column(name = "actor", nullable = false, length = 255, updatable = false)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "request_id", length = 64, updatable = false)
    private String requestId;

    protected OrganizationStatusAuditEntity() {
    }

    public OrganizationStatusAuditEntity(OrganizationStatusAuditEvent event) {
        this.id = event.id();
        this.organizationId = event.organizationId();
        this.action = event.action();
        this.actor = event.actor();
        this.occurredAt = event.occurredAt();
        this.requestId = event.requestId();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public OrganizationStatusAuditAction getAction() {
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
}