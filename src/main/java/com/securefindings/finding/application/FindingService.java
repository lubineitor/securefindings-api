package com.securefindings.finding.application;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
import com.securefindings.finding.api.FindingSortDirection;
import com.securefindings.finding.api.FindingSortField;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.finding.domain.FindingSummary;
import com.securefindings.finding.persistence.FindingEntity;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;

@Service
@Transactional(readOnly = true)
public class FindingService {

        private static final String SYSTEM_ACTOR = "system";
        public static final int MAX_EXPORT_ROWS = 1_000;

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

        public FindingSummary getSummary() {
                return getSummary(null, null);
        }

        public FindingSummary getSummary(
                        Instant createdFrom,
                        Instant createdTo) {

                if (createdFrom != null
                                && createdTo != null
                                && createdFrom.isAfter(createdTo)) {
                        throw new IllegalArgumentException(
                                        "createdFrom no puede ser posterior a createdTo");
                }

                UUID organizationId = organizationContext.currentOrganizationId();

                Map<FindingStatus, Long> byStatus = new EnumMap<>(FindingStatus.class);

                for (FindingStatus status : FindingStatus.values()) {
                        byStatus.put(status, 0L);
                }

                findingRepository
                                .countStatusesByOrganization(
                                                organizationId,
                                                createdFrom,
                                                createdTo)
                                .forEach(result -> byStatus.put(
                                                result.getStatus(),
                                                result.getTotal()));

                Map<FindingSeverity, Long> bySeverity = new EnumMap<>(FindingSeverity.class);

                for (FindingSeverity severity : FindingSeverity.values()) {
                        bySeverity.put(severity, 0L);
                }

                findingRepository
                                .countSeveritiesByOrganization(
                                                organizationId,
                                                createdFrom,
                                                createdTo)
                                .forEach(result -> bySeverity.put(
                                                result.getSeverity(),
                                                result.getTotal()));

                long total = 0L;

                for (Long count : byStatus.values()) {
                        if (count == null) {
                                throw new IllegalStateException(
                                                "El recuento de estados no puede ser nulo");
                        }

                        total += count;
                }

                return new FindingSummary(total, byStatus, bySeverity);
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

        public Page<Finding> findPage(
                        int page,
                        int size,
                        FindingSeverity severity,
                        FindingStatus status) {

                return findPage(
                                page,
                                size,
                                null,
                                severity,
                                status);
        }

        public Page<Finding> findPage(
                        int page,
                        int size,
                        String searchTerm,
                        FindingSeverity severity,
                        FindingStatus status) {

                return findPage(
                                page,
                                size,
                                searchTerm,
                                severity,
                                status,
                                FindingSortField.CREATED_AT,
                                FindingSortDirection.DESC);
        }

        public Page<Finding> findPage(
                        int page,
                        int size,
                        String searchTerm,
                        FindingSeverity severity,
                        FindingStatus status,
                        FindingSortField sortField,
                        FindingSortDirection sortDirection) {

                return findPage(
                                page,
                                size,
                                searchTerm,
                                severity,
                                status,
                                sortField,
                                sortDirection,
                                null,
                                null);
        }

        public Page<Finding> findPage(
                        int page,
                        int size,
                        String searchTerm,
                        FindingSeverity severity,
                        FindingStatus status,
                        FindingSortField sortField,
                        FindingSortDirection sortDirection,
                        Instant createdFrom,
                        Instant createdTo) {

                UUID organizationId = organizationContext.currentOrganizationId();

                Pageable pageable = createPageable(
                                page,
                                size,
                                sortField,
                                sortDirection);

                String normalizedSearchTerm = normalizeSearchTerm(searchTerm);

                Page<FindingEntity> entities = findingRepository
                                .findPageByFilters(
                                                organizationId,
                                                normalizedSearchTerm,
                                                severity,
                                                status,
                                                createdFrom,
                                                createdTo,
                                                pageable);

                return toDomainPage(entities);
        }

        public List<Finding> findForExport(
                        String searchTerm,
                        FindingSeverity severity,
                        FindingStatus status,
                        Instant createdFrom,
                        Instant createdTo) {

                if (createdFrom != null
                                && createdTo != null
                                && createdFrom.isAfter(createdTo)) {
                        throw new IllegalArgumentException(
                                        "createdFrom no puede ser posterior a createdTo");
                }

                UUID organizationId = organizationContext.currentOrganizationId();
                Pageable pageable = PageRequest.of(
                                0,
                                MAX_EXPORT_ROWS + 1,
                                Sort.by(
                                                Sort.Order.desc("createdAt"),
                                                Sort.Order.asc("id")));

                Page<FindingEntity> entities = findingRepository.findPageByFilters(
                                organizationId,
                                normalizeSearchTerm(searchTerm),
                                severity,
                                status,
                                createdFrom,
                                createdTo,
                                pageable);

                if (entities.getTotalElements() > MAX_EXPORT_ROWS) {
                        throw new FindingExportLimitExceededException(MAX_EXPORT_ROWS);
                }

                return toDomainPage(entities).getContent();
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

                if (!currentFinding.status().canTransitionTo(status)) {
                        throw new FindingStatusTransitionException(
                                        id,
                                        currentFinding.status(),
                                        status);
                }

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

        private Pageable createPageable(
                        int page,
                        int size,
                        FindingSortField sortField,
                        FindingSortDirection sortDirection) {

                FindingSortField effectiveSortField = sortField == null
                                ? FindingSortField.CREATED_AT
                                : sortField;

                FindingSortDirection effectiveSortDirection = sortDirection == null
                                ? FindingSortDirection.DESC
                                : sortDirection;

                Sort.Direction springDirection = effectiveSortDirection == FindingSortDirection.ASC
                                ? Sort.Direction.ASC
                                : Sort.Direction.DESC;

                return PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                                new Sort.Order(
                                                                springDirection,
                                                                effectiveSortField.parameter()),
                                                Sort.Order.asc("id")));
        }

        private Page<Finding> toDomainPage(
                        Page<FindingEntity> entities) {

                return entities.map(entity -> Objects.requireNonNull(
                                entity,
                                "El repositorio devolvió una entidad nula")
                                .toDomain());
        }

        private String normalizeSearchTerm(String searchTerm) {
                if (searchTerm == null) {
                        return null;
                }

                String normalizedSearchTerm = searchTerm.trim();

                if (normalizedSearchTerm.isEmpty()) {
                        return null;
                }

                return normalizedSearchTerm
                                .replace("!", "!!")
                                .replace("%", "!%")
                                .replace("_", "!_");
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
