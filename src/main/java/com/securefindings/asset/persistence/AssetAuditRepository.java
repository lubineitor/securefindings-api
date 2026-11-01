package com.securefindings.asset.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.securefindings.asset.domain.AssetAuditAction;

public interface AssetAuditRepository
    extends JpaRepository<AssetAuditEntity, UUID> {

  Page<AssetAuditEntity> findByAssetIdAndOrganizationIdOrderByOccurredAtAscIdAsc(
      UUID assetId,
      UUID organizationId,
      Pageable pageable);

  @Query("""
      SELECT event
      FROM AssetAuditEntity event
      WHERE event.assetId = :assetId
        AND event.organizationId = :organizationId
        AND (:action IS NULL OR event.action = :action)
        AND (:requestId IS NULL OR event.requestId = :requestId)
        AND (:actor IS NULL OR event.actor = :actor)
        AND event.occurredAt >= COALESCE(:occurredFrom, event.occurredAt)
        AND event.occurredAt <= COALESCE(:occurredTo, event.occurredAt)
      """)
  Page<AssetAuditEntity> findPageByAssetIdAndOrganizationIdAndFilters(
      @Param("assetId") UUID assetId,
      @Param("organizationId") UUID organizationId,
      @Param("action") AssetAuditAction action,
      @Param("requestId") String requestId,
      @Param("actor") String actor,
      @Param("occurredFrom") Instant occurredFrom,
      @Param("occurredTo") Instant occurredTo,
      Pageable pageable);
}