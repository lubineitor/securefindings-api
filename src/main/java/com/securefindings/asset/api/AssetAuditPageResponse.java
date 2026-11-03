package com.securefindings.asset.api;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;

import com.securefindings.asset.persistence.AssetAuditEntity;

public record AssetAuditPageResponse(
        List<AssetAuditResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static AssetAuditPageResponse from(Page<AssetAuditEntity> auditPage) {
        List<AssetAuditResponse> content = auditPage.getContent()
                .stream()
                .map(entity -> AssetAuditResponse.from(
                        Objects.requireNonNull(
                                entity,
                                "El repositorio devolvió un evento nulo")))
                .toList();

        return new AssetAuditPageResponse(
                content,
                auditPage.getNumber(),
                auditPage.getSize(),
                auditPage.getTotalElements(),
                auditPage.getTotalPages(),
                auditPage.isFirst(),
                auditPage.isLast());
    }
}