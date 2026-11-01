package com.securefindings.asset.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.persistence.AssetAuditEntity;
import com.securefindings.asset.persistence.AssetAuditRepository;
import com.securefindings.security.OrganizationContext;

@Service
@Transactional(readOnly = true)
public class AssetAuditQueryService {

    private final AssetAuditRepository assetAuditRepository;
    private final OrganizationContext organizationContext;

    public AssetAuditQueryService(
            AssetAuditRepository assetAuditRepository,
            OrganizationContext organizationContext) {
        this.assetAuditRepository = Objects.requireNonNull(assetAuditRepository);
        this.organizationContext = Objects.requireNonNull(organizationContext);
    }

    public Page<AssetAuditEntity> findPageByAssetId(
            UUID assetId,
            int page,
            int size,
            AssetAuditAction action,
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
                page,
                size,
                Sort.by(
                        Sort.Order.asc("occurredAt"),
                        Sort.Order.asc("id")));

        return assetAuditRepository.findPageByAssetIdAndOrganizationIdAndFilters(
                assetId,
                organizationId,
                action,
                requestId,
                actor,
                occurredFrom,
                occurredTo,
                pageable);
    }
}