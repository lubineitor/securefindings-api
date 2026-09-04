package com.securefindings.finding.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.audit.application.AuditService;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@Service
@Transactional(readOnly = true)
public class FindingService {

        private static final String SYSTEM_ACTOR = "system";

        private final FindingRepository findingRepository;
        private final AuditService auditService;
        private final OrganizationContext organizationContext;

        public FindingService(
                        FindingRepository findingRepository,
                        AuditService auditService,
                        OrganizationContext organizationContext) {

                this.findingRepository = Objects.requireNonNull(
                                findingRepository);

                this.auditService = Objects.requireNonNull(
                                auditService);

                this.organizationContext = Objects.requireNonNull(
                                organizationContext);
        }

        @Transactional
        public Finding create(
                        String title,
                        String description,
                        FindingSeverity severity) {

                UUID organizationId = organizationContext.currentOrganizationId();

                Finding finding = Finding.create(
                                title,
                                description,
                                severity);

                Finding savedFinding = findingRepository
                                .save(new FindingEntity(finding, organizationId))
                                .toDomain();

                auditService.register(
                                savedFinding.id(),
                                AuditAction.CREATED,
                                currentActor());

                return savedFinding;
        }

        public List<Finding> findAll() {
                UUID organizationId = organizationContext.currentOrganizationId();

                return findingRepository
                                .findAllByOrganizationId(organizationId)
                                .stream()
                                .map(entity -> Objects.requireNonNull(
                                                entity,
                                                "El repositorio devolvió una entidad nula")
                                                .toDomain())
                                .toList();
        }

        public Page<Finding> findPage(
                        int page,
                        int size,
                        FindingSeverity severity,
                        FindingStatus status) {

                UUID organizationId = organizationContext.currentOrganizationId();

                Pageable pageable = PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                                Sort.Order.desc("createdAt"),
                                                Sort.Order.asc("id")));

                Page<FindingEntity> entities;

                if (severity != null && status != null) {
                        entities = findingRepository
                                        .findByOrganizationIdAndSeverityAndStatus(
                                                        organizationId,
                                                        severity,
                                                        status,
                                                        pageable);
                } else if (severity != null) {
                        entities = findingRepository
                                        .findByOrganizationIdAndSeverity(
                                                        organizationId,
                                                        severity,
                                                        pageable);
                } else if (status != null) {
                        entities = findingRepository
                                        .findByOrganizationIdAndStatus(
                                                        organizationId,
                                                        status,
                                                        pageable);
                } else {
                        entities = findingRepository
                                        .findByOrganizationId(
                                                        organizationId,
                                                        pageable);
                }

                return entities
                                .map(entity -> Objects.requireNonNull(
                                                entity,
                                                "El repositorio devolvió una entidad nula")
                                                .toDomain());
        }

        public Optional<Finding> findById(UUID id) {
                UUID organizationId = organizationContext.currentOrganizationId();

                return findingRepository
                                .findByIdAndOrganizationId(
                                                id,
                                                organizationId)
                                .map(entity -> Objects.requireNonNull(
                                                entity,
                                                "El repositorio devolvió una entidad nula")
                                                .toDomain());
        }

        public Finding getById(UUID id) {
                return findById(id)
                                .orElseThrow(() -> new FindingNotFoundException(id));
        }

        @Transactional
        public Finding updateStatus(
                        UUID id,
                        FindingStatus status) {

                UUID organizationId = organizationContext.currentOrganizationId();

                Finding currentFinding = getById(id);

                Finding updatedFinding = currentFinding.withStatus(status);

                Finding savedFinding = save(
                                updatedFinding,
                                organizationId);

                auditService.register(
                                savedFinding.id(),
                                AuditAction.UPDATED,
                                currentActor());

                return savedFinding;
        }

        @Transactional
        public Finding update(
                        UUID id,
                        String title,
                        String description,
                        FindingSeverity severity) {

                UUID organizationId = organizationContext.currentOrganizationId();

                Finding currentFinding = getById(id);

                Finding updatedFinding = currentFinding.withDetails(
                                title,
                                description,
                                severity);

                Finding savedFinding = save(
                                updatedFinding,
                                organizationId);

                auditService.register(
                                savedFinding.id(),
                                AuditAction.UPDATED,
                                currentActor());

                return savedFinding;
        }

        @Transactional
        public void deleteById(UUID id) {
                UUID organizationId = organizationContext.currentOrganizationId();

                if (!findingRepository.existsByIdAndOrganizationId(
                                id,
                                organizationId)) {

                        throw new FindingNotFoundException(id);
                }

                findingRepository.deleteByIdAndOrganizationId(
                                id,
                                organizationId);

                auditService.register(
                                id,
                                AuditAction.DELETED,
                                currentActor());
        }

        private Finding save(
                        Finding finding,
                        UUID organizationId) {

                return findingRepository
                                .save(new FindingEntity(
                                                finding,
                                                organizationId))
                                .toDomain();
        }

        private String currentActor() {
                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication == null
                                || !authentication.isAuthenticated()) {
                        return SYSTEM_ACTOR;
                }

                if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                        String preferredUsername = jwtAuthentication
                                        .getToken()
                                        .getClaimAsString("preferred_username");

                        if (preferredUsername != null
                                        && !preferredUsername.isBlank()) {
                                return preferredUsername;
                        }
                }

                String name = authentication.getName();

                return name == null || name.isBlank()
                                ? SYSTEM_ACTOR
                                : name;
        }
}