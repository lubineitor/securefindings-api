package com.securefindings.audit.api;

import java.util.List;

import org.springframework.data.domain.Page;

import com.securefindings.audit.persistence.FindingAuditEntity;

public record FindingAuditPageResponse(
        List<FindingAuditResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static FindingAuditPageResponse from(
            Page<FindingAuditEntity> auditPage) {

        List<FindingAuditResponse> content = auditPage
                .getContent()
                .stream()
                .map(FindingAuditResponse::from)
                .toList();

        return new FindingAuditPageResponse(
                content,
                auditPage.getNumber(),
                auditPage.getSize(),
                auditPage.getTotalElements(),
                auditPage.getTotalPages(),
                auditPage.isFirst(),
                auditPage.isLast());
    }
}