package com.securefindings.finding.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

public interface FindingRepository
            extends JpaRepository<FindingEntity, UUID> {

      List<FindingEntity> findAllByOrganizationId(
                  UUID organizationId);

      Optional<FindingEntity> findByIdAndOrganizationId(
                  UUID id,
                  UUID organizationId);

      boolean existsByIdAndOrganizationId(
                  UUID id,
                  UUID organizationId);

      void deleteByIdAndOrganizationId(
                  UUID id,
                  UUID organizationId);

      Page<FindingEntity> findByOrganizationId(
                  UUID organizationId,
                  Pageable pageable);

      Page<FindingEntity> findByOrganizationIdAndSeverity(
                  UUID organizationId,
                  FindingSeverity severity,
                  Pageable pageable);

      Page<FindingEntity> findByOrganizationIdAndStatus(
                  UUID organizationId,
                  FindingStatus status,
                  Pageable pageable);

      Page<FindingEntity> findByOrganizationIdAndSeverityAndStatus(
                  UUID organizationId,
                  FindingSeverity severity,
                  FindingStatus status,
                  Pageable pageable);

      @Query("""
                  SELECT finding
                  FROM FindingEntity finding
                  WHERE finding.organizationId = :organizationId
                    AND (
                          COALESCE(:searchTerm, '') = ''
                          OR LOWER(finding.title) LIKE LOWER(
                                  CONCAT('%', COALESCE(:searchTerm, ''), '%'))
                          OR LOWER(finding.description) LIKE LOWER(
                                  CONCAT('%', COALESCE(:searchTerm, ''), '%'))
                    )
                    AND (:severity IS NULL OR finding.severity = :severity)
                    AND (:status IS NULL OR finding.status = :status)
                  """)
      Page<FindingEntity> findPageByFilters(
                  @Param("organizationId") UUID organizationId,
                  @Param("searchTerm") String searchTerm,
                  @Param("severity") FindingSeverity severity,
                  @Param("status") FindingStatus status,
                  Pageable pageable);

}