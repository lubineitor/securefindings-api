package com.securefindings.asset.persistence;

import java.time.Instant;
import java.util.UUID;

import com.securefindings.asset.domain.Asset;
import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.asset.domain.AssetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assets")
public class AssetEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "identifier", nullable = false, length = 255)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    private AssetType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 30)
    private AssetEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "criticality", nullable = false, length = 20)
    private AssetCriticality criticality;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssetStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AssetEntity() {
    }

    public AssetEntity(Asset asset, UUID organizationId) {
        this.id = asset.id();
        this.organizationId = organizationId;
        this.name = asset.name();
        this.identifier = asset.identifier();
        this.type = asset.type();
        this.environment = asset.environment();
        this.criticality = asset.criticality();
        this.status = asset.status();
        this.createdAt = asset.createdAt();
        this.updatedAt = asset.updatedAt();
    }

    public Asset toDomain() {
        return new Asset(
                id,
                name,
                identifier,
                type,
                environment,
                criticality,
                status,
                createdAt,
                updatedAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public AssetStatus getStatus() {
        return status;
    }
}