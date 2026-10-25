package com.securefindings.asset.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.asset.domain.AssetType;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {

    Optional<AssetEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndIdentifierIgnoreCase(
            UUID organizationId,
            String identifier);

    boolean existsByOrganizationIdAndIdentifierIgnoreCaseAndIdNot(
            UUID organizationId,
            String identifier,
            UUID id);

    @Query("""
            SELECT asset
            FROM AssetEntity asset
            WHERE asset.organizationId = :organizationId
              AND (
                    COALESCE(:searchTerm, '') = ''
                    OR LOWER(asset.name) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) ESCAPE '!'
                    OR LOWER(asset.identifier) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) ESCAPE '!'
              )
              AND (:type IS NULL OR asset.type = :type)
              AND (:environment IS NULL OR asset.environment = :environment)
              AND (:criticality IS NULL OR asset.criticality = :criticality)
              AND (:status IS NULL OR asset.status = :status)
            """)
    Page<AssetEntity> findPageByOrganizationAndFilters(
            @Param("organizationId") UUID organizationId,
            @Param("searchTerm") String searchTerm,
            @Param("type") AssetType type,
            @Param("environment") AssetEnvironment environment,
            @Param("criticality") AssetCriticality criticality,
            @Param("status") AssetStatus status,
            Pageable pageable);
}