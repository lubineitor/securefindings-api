package com.securefindings.organization.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.domain.OrganizationStatus;
import com.securefindings.organization.persistence.OrganizationEntity;
import com.securefindings.organization.persistence.OrganizationRepository;

@Service
@Transactional(readOnly = true)
public class OrganizationLifecycleService {

    private final OrganizationRepository organizationRepository;

    public OrganizationLifecycleService(
            OrganizationRepository organizationRepository) {
        this.organizationRepository = Objects.requireNonNull(organizationRepository);
    }

    @Transactional
    public Organization changeStatus(
            UUID organizationId,
            OrganizationStatus status) {

        UUID checkedId = Objects.requireNonNull(organizationId);
        OrganizationStatus checkedStatus = Objects.requireNonNull(status);

        Organization current = organizationRepository.findById(checkedId)
                .map(entity -> Objects.requireNonNull(
                        entity,
                        "El repositorio devolvió una organización nula").toDomain())
                .orElseThrow(() -> new OrganizationNotFoundException(checkedId));

        if (current.status() == checkedStatus) {
            return current;
        }

        Organization updated = new Organization(
                current.id(),
                current.name(),
                current.slug(),
                current.createdAt(),
                checkedStatus);

        OrganizationEntity saved = organizationRepository.save(
                new OrganizationEntity(updated));

        return Objects.requireNonNull(
                saved,
                "El repositorio devolvió una organización nula").toDomain();
    }
}