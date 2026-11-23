package com.securefindings.organization.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.organization.domain.Organization;
import com.securefindings.organization.domain.OrganizationStatus;
import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.domain.OrganizationStatusAuditEvent;
import com.securefindings.organization.persistence.OrganizationEntity;
import com.securefindings.organization.persistence.OrganizationRepository;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;
import com.securefindings.organization.persistence.OrganizationStatusAuditRepository;
import com.securefindings.security.RequestCorrelationFilter;

@Service
@Transactional(readOnly = true)
public class OrganizationLifecycleService {

        private final OrganizationRepository organizationRepository;
        private final OrganizationStatusAuditRepository auditRepository;

        public OrganizationLifecycleService(
                        OrganizationRepository organizationRepository,
                        OrganizationStatusAuditRepository auditRepository) {
                this.organizationRepository = Objects.requireNonNull(organizationRepository);
                this.auditRepository = Objects.requireNonNull(auditRepository);
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
                                                "El repositorio devolvió una organización nula")
                                                .toDomain())
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

                OrganizationStatusAuditAction action = checkedStatus == OrganizationStatus.SUSPENDED
                                ? OrganizationStatusAuditAction.SUSPENDED
                                : OrganizationStatusAuditAction.REACTIVATED;

                OrganizationStatusAuditEvent event = new OrganizationStatusAuditEvent(
                                UUID.randomUUID(),
                                checkedId,
                                action,
                                currentActor(),
                                Instant.now(),
                                RequestCorrelationFilter.currentRequestId());

                auditRepository.save(new OrganizationStatusAuditEntity(event));

                return Objects.requireNonNull(
                                saved,
                                "El repositorio devolvió una organización nula")
                                .toDomain();
        }

        private String currentActor() {
                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication == null
                                || !authentication.isAuthenticated()
                                || authentication.getName() == null
                                || authentication.getName().isBlank()
                                || "anonymousUser".equals(authentication.getName())) {
                        return "system";
                }

                return authentication.getName();
        }
}