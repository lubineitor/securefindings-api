package com.securefindings.asset.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetAuditRepository
        extends JpaRepository<AssetAuditEntity, UUID> {

    Page<AssetAuditEntity> findByAssetIdAndOrganizationIdOrderByOccurredAtAscIdAsc(
            UUID assetId,
            UUID organizationId,
            Pageable pageable);

    boolean existsByOrganizationId(UUID organizationId);
}