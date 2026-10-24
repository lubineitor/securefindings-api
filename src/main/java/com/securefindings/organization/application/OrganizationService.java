package com.securefindings.organization.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.audit.persistence.FindingAuditRepository;
import com.securefindings.comment.persistence.FindingCommentRepository;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.persistence.OrganizationEntity;
import com.securefindings.organization.persistence.OrganizationRepository;
import com.securefindings.security.OrganizationContext;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

        private final OrganizationRepository organizationRepository;
        private final OrganizationContext organizationContext;
        private final FindingRepository findingRepository;
        private final FindingCommentRepository findingCommentRepository;
        private final FindingAuditRepository findingAuditRepository;

        public OrganizationService(
                        OrganizationRepository organizationRepository,
                        OrganizationContext organizationContext,
                        FindingRepository findingRepository,
                        FindingCommentRepository findingCommentRepository,
                        FindingAuditRepository findingAuditRepository) {

                this.organizationRepository = Objects.requireNonNull(organizationRepository);
                this.organizationContext = Objects.requireNonNull(organizationContext);
                this.findingRepository = Objects.requireNonNull(findingRepository);
                this.findingCommentRepository = Objects.requireNonNull(findingCommentRepository);
                this.findingAuditRepository = Objects.requireNonNull(findingAuditRepository);
        }

        public Organization getCurrentOrganization() {
                UUID organizationId = organizationContext.currentOrganizationId();

                return findOrganization(organizationId);
        }

        public Page<Organization> findOrganizations(
                        int page,
                        int size,
                        String query) {
                String normalizedQuery = query == null ? null : query.trim();
                var pageable = PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                                Sort.Order.asc("name"),
                                                Sort.Order.asc("id")));

                Page<OrganizationEntity> organizations = normalizedQuery == null
                                || normalizedQuery.isEmpty()
                                                ? organizationRepository.findAll(pageable)
                                                : organizationRepository
                                                                .findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(
                                                                                normalizedQuery,
                                                                                normalizedQuery,
                                                                                pageable);

                return organizations.map(entity -> Objects.requireNonNull(
                                entity,
                                "El repositorio devolvió una organización nula")
                                .toDomain());
        }

        public Organization getOrganization(UUID organizationId) {
                return organizationRepository
                                .findById(Objects.requireNonNull(organizationId))
                                .map(entity -> Objects.requireNonNull(
                                                entity,
                                                "El repositorio devolvió una organización nula")
                                                .toDomain())
                                .orElseThrow(() -> new OrganizationNotFoundException(
                                                organizationId));
        }

        @Transactional
        public Organization updateOrganization(UUID organizationId, String name) {
                Organization organization = getOrganization(organizationId);

                return updateOrganizationName(organization, name);
        }

        @Transactional
        public Organization createOrganization(String name, String slug) {
                String normalizedSlug = Objects.requireNonNull(slug).trim();

                if (organizationRepository.existsBySlug(normalizedSlug)) {
                        throw new OrganizationSlugAlreadyExistsException(normalizedSlug);
                }

                Organization organization = new Organization(
                                UUID.randomUUID(),
                                Objects.requireNonNull(name).trim(),
                                normalizedSlug,
                                Instant.now());

                try {
                        OrganizationEntity savedEntity = organizationRepository.saveAndFlush(
                                        new OrganizationEntity(organization));

                        return Objects.requireNonNull(
                                        savedEntity,
                                        "El repositorio devolvió una organización nula")
                                        .toDomain();
                } catch (DataIntegrityViolationException exception) {
                        throw new OrganizationSlugAlreadyExistsException(
                                        normalizedSlug,
                                        exception);
                }
        }

        @Transactional
        public Organization updateCurrentOrganizationName(String name) {
                UUID organizationId = organizationContext.currentOrganizationId();
                Organization currentOrganization = findOrganization(organizationId);

                return updateOrganizationName(currentOrganization, name);
        }

        private Organization updateOrganizationName(
                        Organization organization,
                        String name) {
                Organization updatedOrganization = new Organization(
                                organization.id(),
                                Objects.requireNonNull(name).trim(),
                                organization.slug(),
                                organization.createdAt());

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

        @Transactional
        public void deleteOrganization(UUID organizationId) {
                UUID checkedOrganizationId = Objects.requireNonNull(organizationId);
                getOrganization(checkedOrganizationId);

                if (findingRepository.existsByOrganizationId(checkedOrganizationId)
                                || findingCommentRepository.existsByOrganizationId(checkedOrganizationId)
                                || findingAuditRepository.existsByOrganizationId(checkedOrganizationId)) {
                        throw new OrganizationHasAssociatedDataException(checkedOrganizationId);
                }

                try {
                        organizationRepository.deleteById(checkedOrganizationId);
                        organizationRepository.flush();
                } catch (DataIntegrityViolationException exception) {
                        // La clave foránea de PostgreSQL protege también ante escrituras concurrentes.
                        throw new OrganizationHasAssociatedDataException(
                                        checkedOrganizationId,
                                        exception);
                }
        }
}
