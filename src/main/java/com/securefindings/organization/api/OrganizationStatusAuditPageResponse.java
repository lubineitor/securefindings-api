package com.securefindings.organization.api;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;

import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;

public record OrganizationStatusAuditPageResponse(
        List<OrganizationStatusAuditResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static OrganizationStatusAuditPageResponse from(
            Page<OrganizationStatusAuditEntity> auditPage) {

        Objects.requireNonNull(auditPage);

        List<OrganizationStatusAuditResponse> content = auditPage.getContent()
                .stream()
                .map(OrganizationStatusAuditResponse::from)
                .toList();

        return new OrganizationStatusAuditPageResponse(
                content,
                auditPage.getNumber(),
                auditPage.getSize(),
                auditPage.getTotalElements(),
                auditPage.getTotalPages(),
                auditPage.isFirst(),
                auditPage.isLast());
    }
}