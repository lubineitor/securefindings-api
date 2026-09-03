package com.securefindings.finding.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

public interface FindingRepository
                extends JpaRepository<FindingEntity, UUID> {

        Page<FindingEntity> findBySeverity(
                        FindingSeverity severity,
                        Pageable pageable);

        Page<FindingEntity> findByStatus(
                        FindingStatus status,
                        Pageable pageable);

        Page<FindingEntity> findBySeverityAndStatus(
                        FindingSeverity severity,
                        FindingStatus status,
                        Pageable pageable);
}