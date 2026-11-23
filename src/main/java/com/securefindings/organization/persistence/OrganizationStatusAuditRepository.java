package com.securefindings.organization.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationStatusAuditRepository
        extends JpaRepository<OrganizationStatusAuditEntity, UUID> {

    Page<OrganizationStatusAuditEntity> findByOrganizationId(
            UUID organizationId,
            Pageable pageable);
}