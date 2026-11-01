package com.securefindings.asset.persistence;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.domain.AssetAuditEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "asset_audit")
public class AssetAuditEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "asset_id", nullable = false, updatable = false)
    private UUID assetId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20, updatable = false)
    private AssetAuditAction action;

    @Column(name = "actor", nullable = false, length = 255, updatable = false)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "request_id", length = 64, updatable = false)
    private String requestId;

    protected AssetAuditEntity() {
    }

    public AssetAuditEntity(AssetAuditEvent event) {
        this.id = event.id();
        this.assetId = event.assetId();
        this.organizationId = event.organizationId();
        this.action = event.action();
        this.actor = event.actor();
        this.occurredAt = event.occurredAt();
        this.requestId = event.requestId();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssetId() {
        return assetId;
    }

    @JsonIgnore
    public UUID getOrganizationId() {
        return organizationId;
    }

    public AssetAuditAction getAction() {
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