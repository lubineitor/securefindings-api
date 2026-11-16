package com.securefindings.audit.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.domain.AuditEvent;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.audit.persistence.FindingAuditRepository;
import com.securefindings.finding.application.FindingNotFoundException;
import com.securefindings.finding.persistence.FindingRepository;
import com.securefindings.security.OrganizationContext;
import com.securefindings.security.RequestCorrelationFilter;

import jakarta.persistence.criteria.Predicate;

@Service
public class AuditService {

        public static final int MAX_EXPORT_ROWS = 1000;

        private final FindingAuditRepository auditRepository;
        private final FindingRepository findingRepository;
        private final OrganizationContext organizationContext;

        public AuditService(
                        FindingAuditRepository auditRepository,
                        FindingRepository findingRepository,
                        OrganizationContext organizationContext) {

                this.auditRepository = Objects.requireNonNull(auditRepository);
                this.findingRepository = Objects.requireNonNull(findingRepository);
                this.organizationContext = Objects.requireNonNull(organizationContext);
        }

        @Transactional
        public void register(
                        UUID findingId,
                        AuditAction action,
                        String actor) {

                register(
                                findingId,
                                action,
                                actor,
                                RequestCorrelationFilter.currentRequestId());
        }

        private void ensureFindingCanBeAudited(
                        UUID findingId,
                        UUID organizationId,
                        AuditAction action) {

                Objects.requireNonNull(
                                action,
                                "La acción de auditoría no puede ser nula");

                boolean findingExists = findingRepository
                                .findByIdAndOrganizationId(
                                                findingId,
                                                organizationId)
                                .isPresent();

                if (findingExists) {
                        return;
                }

                boolean deletedFindingHasAudit = action == AuditAction.DELETED
                                && auditRepository
                                                .existsByFindingIdAndOrganizationId(
                                                                findingId,
                                                                organizationId);

                if (!deletedFindingHasAudit) {
                        throw new FindingNotFoundException(findingId);
                }
        }

        @Transactional
        public void register(
                        UUID findingId,
                        AuditAction action,
                        String actor,
                        String requestId) {

                UUID organizationId = organizationContext.currentOrganizationId();

                ensureFindingCanBeAudited(
                                findingId,
                                organizationId,
                                action);

                AuditEvent event = new AuditEvent(
                                findingId,
                                organizationId,
                                action,
                                actor,
                                Instant.now(),
                                requestId);

                FindingAuditEntity entity = new FindingAuditEntity(
                                UUID.randomUUID(),
                                event);

                auditRepository.save(entity);
        }

        @Transactional(readOnly = true)
        public Page<FindingAuditEntity> findPageByFindingId(
                        UUID findingId,
                        int page,
                        int size) {

                UUID organizationId = ensureFindingBelongsToOrganization(findingId);

                Pageable pageable = createPageable(page, size);

                return auditRepository
                                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                                                findingId,
                                                organizationId,
                                                pageable);
        }

        @Transactional(readOnly = true)
        public Page<FindingAuditEntity> findPageByFindingId(
                        UUID findingId,
                        int page,
                        int size,
                        AuditAction action,
                        String requestId) {

                return findPageByFindingId(
                                findingId,
                                page,
                                size,
                                action,
                                requestId,
                                null);
        }

        @Transactional(readOnly = true)
        public Page<FindingAuditEntity> findPageByFindingId(
                        UUID findingId,
                        int page,
                        int size,
                        AuditAction action,
                        String requestId,
                        String actor) {

                UUID organizationId = ensureFindingBelongsToOrganization(findingId);
                Pageable pageable = createPageable(page, size);

                if (action != null && requestId != null && actor != null) {
                        return auditRepository
                                        .findByFindingIdAndOrganizationIdAndActionAndRequestIdAndActorOrderByOccurredAtAsc(
                                                        findingId,
                                                        organizationId,
                                                        action,
                                                        requestId,
                                                        actor,
                                                        pageable);
                }

                if (action != null && requestId != null) {
                        return auditRepository
                                        .findByFindingIdAndOrganizationIdAndActionAndRequestIdOrderByOccurredAtAsc(
                                                        findingId,
                                                        organizationId,
                                                        action,
                                                        requestId,
                                                        pageable);
                }

                if (action != null && actor != null) {
                        return auditRepository
                                        .findByFindingIdAndOrganizationIdAndActionAndActorOrderByOccurredAtAsc(
                                                        findingId,
                                                        organizationId,
                                                        action,
                                                        actor,
                                                        pageable);
                }

                if (requestId != null && actor != null) {
                        return auditRepository
                                        .findByFindingIdAndOrganizationIdAndRequestIdAndActorOrderByOccurredAtAsc(
                                                        findingId,
                                                        organizationId,
                                                        requestId,
                                                        actor,
                                                        pageable);
                }

                if (action != null) {
                        return auditRepository
                                        .findByFindingIdAndOrganizationIdAndActionOrderByOccurredAtAsc(
                                                        findingId,
                                                        organizationId,
                                                        action,
                                                        pageable);
                }

                if (requestId != null) {
                        return auditRepository
                                        .findByFindingIdAndOrganizationIdAndRequestIdOrderByOccurredAtAsc(
                                                        findingId,
                                                        organizationId,
                                                        requestId,
                                                        pageable);
                }

                if (actor != null) {
                        return auditRepository
                                        .findByFindingIdAndOrganizationIdAndActorOrderByOccurredAtAsc(
                                                        findingId,
                                                        organizationId,
                                                        actor,
                                                        pageable);
                }

                return auditRepository
                                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                                                findingId,
                                                organizationId,
                                                pageable);
        }

        private UUID ensureFindingBelongsToOrganization(UUID findingId) {
                UUID organizationId = organizationContext.currentOrganizationId();

                boolean findingExists = findingRepository
                                .findByIdAndOrganizationId(
                                                findingId,
                                                organizationId)
                                .isPresent();

                boolean auditExists = auditRepository
                                .existsByFindingIdAndOrganizationId(
                                                findingId,
                                                organizationId);

                if (!findingExists && !auditExists) {
                        throw new FindingNotFoundException(findingId);
                }

                return organizationId;
        }

        private Pageable createPageable(
                        int page,
                        int size) {

                return PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                                Sort.Order.asc("occurredAt"),
                                                Sort.Order.asc("id")));
        }

        @Transactional(readOnly = true)
        public Page<FindingAuditEntity> findPageByFindingId(
                        UUID findingId,
                        int page,
                        int size,
                        AuditAction action,
                        String requestId,
                        String actor,
                        Instant occurredFrom,
                        Instant occurredTo) {

                if (occurredFrom != null
                                && occurredTo != null
                                && occurredFrom.isAfter(occurredTo)) {
                        throw new IllegalArgumentException(
                                        "occurredFrom debe ser anterior o igual a occurredTo");
                }

                if (occurredFrom == null && occurredTo == null) {
                        return findPageByFindingId(
                                        findingId,
                                        page,
                                        size,
                                        action,
                                        requestId,
                                        actor);
                }

                UUID organizationId = ensureFindingBelongsToOrganization(findingId);
                Pageable pageable = createPageable(page, size);

                Specification<FindingAuditEntity> specification = (root, query, criteriaBuilder) -> {
                        Predicate predicate = criteriaBuilder.and(
                                        criteriaBuilder.equal(root.get("findingId"), findingId),
                                        criteriaBuilder.equal(root.get("organizationId"), organizationId));

                        if (action != null) {
                                predicate = criteriaBuilder.and(
                                                predicate,
                                                criteriaBuilder.equal(root.get("action"), action));
                        }

                        if (requestId != null) {
                                predicate = criteriaBuilder.and(
                                                predicate,
                                                criteriaBuilder.equal(root.get("requestId"), requestId));
                        }

                        if (actor != null) {
                                predicate = criteriaBuilder.and(
                                                predicate,
                                                criteriaBuilder.equal(root.get("actor"), actor));
                        }

                        if (occurredFrom != null) {
                                predicate = criteriaBuilder.and(
                                                predicate,
                                                criteriaBuilder.greaterThanOrEqualTo(
                                                                root.<Instant>get("occurredAt"),
                                                                occurredFrom));
                        }

                        if (occurredTo != null) {
                                predicate = criteriaBuilder.and(
                                                predicate,
                                                criteriaBuilder.lessThanOrEqualTo(
                                                                root.<Instant>get("occurredAt"),
                                                                occurredTo));
                        }

                        return predicate;
                };

                return auditRepository.findAll(specification, pageable);
        }

        @Transactional(readOnly = true)
        public List<FindingAuditEntity> findForOrganizationExport(
                        AuditAction action,
                        String requestId,
                        String actor,
                        Instant occurredFrom,
                        Instant occurredTo) {

                if (occurredFrom != null
                                && occurredTo != null
                                && occurredFrom.isAfter(occurredTo)) {
                        throw new IllegalArgumentException(
                                        "occurredFrom debe ser anterior o igual a occurredTo");
                }

                UUID organizationId = organizationContext.currentOrganizationId();

                Pageable pageable = PageRequest.of(
                                0,
                                MAX_EXPORT_ROWS + 1,
                                Sort.by(
                                                Sort.Order.asc("occurredAt"),
                                                Sort.Order.asc("id")));

                Page<FindingAuditEntity> auditPage = auditRepository.findPageByOrganizationIdAndFilters(
                                organizationId,
                                action,
                                requestId,
                                actor,
                                occurredFrom,
                                occurredTo,
                                pageable);

                if (auditPage.getTotalElements() > MAX_EXPORT_ROWS
                                || auditPage.getContent().size() > MAX_EXPORT_ROWS) {
                        throw new AuditExportLimitExceededException(MAX_EXPORT_ROWS);
                }

                return List.copyOf(auditPage.getContent());
        }

        @Transactional(readOnly = true)
        public Page<FindingAuditEntity> findPageByOrganization(
                        int page,
                        int size,
                        AuditAction action,
                        String requestId,
                        String actor,
                        Instant occurredFrom,
                        Instant occurredTo) {

                if (occurredFrom != null
                                && occurredTo != null
                                && occurredFrom.isAfter(occurredTo)) {
                        throw new IllegalArgumentException(
                                        "occurredFrom debe ser anterior o igual a occurredTo");
                }

                UUID organizationId = organizationContext.currentOrganizationId();
                Pageable pageable = createPageable(page, size);

                return auditRepository.findPageByOrganizationIdAndFilters(
                                organizationId,
                                action,
                                requestId,
                                actor,
                                occurredFrom,
                                occurredTo,
                                pageable);
        }
}
