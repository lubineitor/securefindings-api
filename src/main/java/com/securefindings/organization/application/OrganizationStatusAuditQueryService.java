package com.securefindings.organization.application;

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

import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;
import com.securefindings.organization.persistence.OrganizationStatusAuditRepository;

@Service
@Transactional(readOnly = true)
public class OrganizationStatusAuditQueryService {

    public static final int MAX_EXPORT_ROWS = 1000;

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

    public List<OrganizationStatusAuditEntity> findForExport(
            UUID organizationId,
            OrganizationStatusAuditAction action,
            String requestId,
            String actor,
            Instant occurredFrom,
            Instant occurredTo) {

        UUID checkedOrganizationId = Objects.requireNonNull(organizationId);
        validateDateRange(occurredFrom, occurredTo);

        Pageable pageable = PageRequest.of(
                0,
                MAX_EXPORT_ROWS + 1,
                Sort.by(
                        Sort.Order.asc("occurredAt"),
                        Sort.Order.asc("id")));

        Page<OrganizationStatusAuditEntity> auditPage = auditRepository.findPageByOrganizationIdAndFilters(
                checkedOrganizationId,
                action,
                requestId,
                actor,
                occurredFrom,
                occurredTo,
                pageable);

        if (auditPage.getTotalElements() > MAX_EXPORT_ROWS
                || auditPage.getContent().size() > MAX_EXPORT_ROWS) {
            throw new OrganizationStatusAuditExportLimitExceededException(
                    MAX_EXPORT_ROWS);
        }

        return auditPage.getContent();
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