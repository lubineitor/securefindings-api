package com.securefindings.finding.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

@Entity
@Table(name = "findings")
public class FindingEntity {

    private static final UUID SYSTEM_ORGANIZATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private FindingSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FindingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FindingEntity() {
    }

    public FindingEntity(Finding finding) {
        this(finding, SYSTEM_ORGANIZATION_ID);
    }

    public FindingEntity(
            Finding finding,
            UUID organizationId) {

        this.id = finding.id();
        this.organizationId = organizationId;
        this.title = finding.title();
        this.description = finding.description();
        this.severity = finding.severity();
        this.status = finding.status();
        this.createdAt = finding.createdAt();
        this.updatedAt = finding.updatedAt();
    }

    public Finding toDomain() {
        return new Finding(
                id,
                title,
                description,
                severity,
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public FindingSeverity getSeverity() {
        return severity;
    }

    public FindingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}