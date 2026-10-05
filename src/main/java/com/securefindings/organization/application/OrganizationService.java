package com.securefindings.organization.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.persistence.OrganizationEntity;
import com.securefindings.organization.persistence.OrganizationRepository;
import com.securefindings.security.OrganizationContext;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

        private final OrganizationRepository organizationRepository;
        private final OrganizationContext organizationContext;

        public OrganizationService(
                        OrganizationRepository organizationRepository,
                        OrganizationContext organizationContext) {

                this.organizationRepository = Objects.requireNonNull(
                                organizationRepository);

                this.organizationContext = Objects.requireNonNull(
                                organizationContext);
        }

        public Organization getCurrentOrganization() {
                UUID organizationId = organizationContext.currentOrganizationId();

                return findOrganization(organizationId);
        }

        @Transactional
        public Organization updateCurrentOrganizationName(String name) {
                UUID organizationId = organizationContext.currentOrganizationId();
                Organization currentOrganization = findOrganization(organizationId);

                Organization updatedOrganization = new Organization(
                                currentOrganization.id(),
                                Objects.requireNonNull(name).trim(),
                                currentOrganization.slug(),
                                currentOrganization.createdAt());

                OrganizationEntity savedEntity = organizationRepository.save(
                                new OrganizationEntity(updatedOrganization));

                return Objects.requireNonNull(
                                savedEntity,
                                "El repositorio devolvió una organización nula")
                                .toDomain();
        }

        private Organization findOrganization(UUID organizationId) {
                return organizationRepository
                                .findById(organizationId)
                                .map(entity -> Objects.requireNonNull(
                                                entity,
                                                "El repositorio devolvió una organización nula")
                                                .toDomain())
                                .orElseThrow(() -> new AccessDeniedException(
                                                "La organización del token no existe"));
        }
}