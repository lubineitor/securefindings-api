package com.securefindings.organization.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;
import com.securefindings.organization.persistence.OrganizationStatusAuditRepository;

@Service
@Transactional(readOnly = true)
public class OrganizationStatusAuditQueryService {

    private final OrganizationStatusAuditRepository auditRepository;

    public OrganizationStatusAuditQueryService(
            OrganizationStatusAuditRepository auditRepository) {
        this.auditRepository = Objects.requireNonNull(auditRepository);
    }

    public Page<OrganizationStatusAuditEntity> findPageByOrganizationId(
            UUID organizationId,
            int page,
            int size,
            OrganizationStatusAuditAction action,
            String requestId,
            String actor,
            Instant occurredFrom,
            Instant occurredTo) {

        UUID checkedOrganizationId = Objects.requireNonNull(organizationId);
        validateDateRange(occurredFrom, occurredTo);

        return auditRepository.findPageByOrganizationIdAndFilters(
                checkedOrganizationId,
                action,
                requestId,
                actor,
                occurredFrom,
                occurredTo,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("occurredAt"),
                                Sort.Order.desc("id"))));
    }

    private void validateDateRange(
            Instant occurredFrom,
            Instant occurredTo) {
        if (occurredFrom != null
                && occurredTo != null
                && occurredFrom.isAfter(occurredTo)) {
            throw new IllegalArgumentException(
                    "occurredFrom debe ser anterior o igual a occurredTo");
        }
    }
}