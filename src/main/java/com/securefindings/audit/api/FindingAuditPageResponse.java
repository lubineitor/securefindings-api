package com.securefindings.audit.api;

import java.util.List;

import org.springframework.data.domain.Page;

import com.securefindings.audit.persistence.FindingAuditEntity;

public record FindingAuditPageResponse(
        List<FindingAuditEntity> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static FindingAuditPageResponse from(
            Page<FindingAuditEntity> auditPage) {

        return new FindingAuditPageResponse(
                auditPage.getContent(),
                auditPage.getNumber(),
                auditPage.getSize(),
                auditPage.getTotalElements(),
                auditPage.getTotalPages(),
                auditPage.isFirst(),
                auditPage.isLast());
    }
}