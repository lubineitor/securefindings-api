package com.securefindings.organization.persistence;

import java.time.Instant;
import java.util.UUID;

import com.securefindings.organization.domain.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class OrganizationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrganizationEntity() {
    }

    public OrganizationEntity(Organization organization) {
        this.id = organization.id();
        this.name = organization.name();
        this.slug = organization.slug();
        this.createdAt = organization.createdAt();
    }

    public Organization toDomain() {
        return new Organization(
                id,
                name,
                slug,
                createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}