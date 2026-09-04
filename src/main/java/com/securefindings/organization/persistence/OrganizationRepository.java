package com.securefindings.organization.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository
        extends JpaRepository<OrganizationEntity, UUID> {
}