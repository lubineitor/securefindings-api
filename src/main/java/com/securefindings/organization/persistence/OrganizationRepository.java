package com.securefindings.organization.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.securefindings.organization.domain.OrganizationStatus;

public interface OrganizationRepository
                extends JpaRepository<OrganizationEntity, UUID> {

        boolean existsBySlug(String slug);

        Page<OrganizationEntity> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(
                        String name,
                        String slug,
                        Pageable pageable);

        boolean existsByIdAndStatus(UUID id, OrganizationStatus status);
}