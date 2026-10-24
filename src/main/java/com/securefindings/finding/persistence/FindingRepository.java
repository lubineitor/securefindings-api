package com.securefindings.finding.persistence;

import java.time.Instant;
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

        Optional<FindingEntity> findByIdAndOrganizationId(
                        UUID id,
                        UUID organizationId);

        boolean existsByIdAndOrganizationId(
                        UUID id,
                        UUID organizationId);

        void deleteByIdAndOrganizationId(
                        UUID id,
                        UUID organizationId);

        @Query("""
                        SELECT finding
                        FROM FindingEntity finding
                        WHERE finding.organizationId = :organizationId
                          AND (
                                COALESCE(:searchTerm, '') = ''
                                OR LOWER(finding.title) LIKE LOWER(
                                        CONCAT('%', COALESCE(:searchTerm, ''), '%')) ESCAPE '!'
                                OR LOWER(finding.description) LIKE LOWER(
                                        CONCAT('%', COALESCE(:searchTerm, ''), '%')) ESCAPE '!'
                          )
                          AND (:severity IS NULL OR finding.severity = :severity)
                          AND (:status IS NULL OR finding.status = :status)
                          AND finding.createdAt >= COALESCE(:createdFrom, finding.createdAt)
                          AND finding.createdAt <= COALESCE(:createdTo, finding.createdAt)
                        """)
        Page<FindingEntity> findPageByFilters(
                        @Param("organizationId") UUID organizationId,
                        @Param("searchTerm") String searchTerm,
                        @Param("severity") FindingSeverity severity,
                        @Param("status") FindingStatus status,
                        @Param("createdFrom") Instant createdFrom,
                        @Param("createdTo") Instant createdTo,
                        Pageable pageable);

        interface StatusCountProjection {
                FindingStatus getStatus();

                Long getTotal();
        }

        interface SeverityCountProjection {
                FindingSeverity getSeverity();

                Long getTotal();
        }

        @Query("""
                        SELECT finding.status AS status, COUNT(finding.id) AS total
                        FROM FindingEntity finding
                        WHERE finding.organizationId = :organizationId
                          AND finding.createdAt >= COALESCE(:createdFrom, finding.createdAt)
                          AND finding.createdAt <= COALESCE(:createdTo, finding.createdAt)
                        GROUP BY finding.status
                        """)
        List<StatusCountProjection> countStatusesByOrganization(
                        @Param("organizationId") UUID organizationId,
                        @Param("createdFrom") Instant createdFrom,
                        @Param("createdTo") Instant createdTo);

        @Query("""
                        SELECT finding.severity AS severity, COUNT(finding.id) AS total
                        FROM FindingEntity finding
                        WHERE finding.organizationId = :organizationId
                          AND finding.createdAt >= COALESCE(:createdFrom, finding.createdAt)
                          AND finding.createdAt <= COALESCE(:createdTo, finding.createdAt)
                        GROUP BY finding.severity
                        """)
        List<SeverityCountProjection> countSeveritiesByOrganization(
                        @Param("organizationId") UUID organizationId,
                        @Param("createdFrom") Instant createdFrom,
                        @Param("createdTo") Instant createdTo);

        boolean existsByOrganizationId(UUID organizationId);
}