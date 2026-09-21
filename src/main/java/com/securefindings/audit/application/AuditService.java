package com.securefindings.audit.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@Service
public class AuditService {

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

        @Transactional
        public void register(
                        UUID findingId,
                        AuditAction action,
                        String actor,
                        String requestId) {

                UUID organizationId = organizationContext.currentOrganizationId();

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
        public List<FindingAuditEntity> findByFindingId(
                        UUID findingId) {

                UUID organizationId = ensureFindingBelongsToOrganization(findingId);

                return auditRepository
                                .findByFindingIdAndOrganizationIdOrderByOccurredAtAsc(
                                                findingId,
                                                organizationId);
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

                UUID organizationId = ensureFindingBelongsToOrganization(findingId);

                Pageable pageable = createPageable(page, size);

                if (action != null && requestId != null) {
                        return auditRepository
                                        .findByFindingIdAndOrganizationIdAndActionAndRequestIdOrderByOccurredAtAsc(
                                                        findingId,
                                                        organizationId,
                                                        action,
                                                        requestId,
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
}
