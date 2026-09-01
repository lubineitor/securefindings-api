package com.securefindings.finding.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository
        extends JpaRepository<FindingEntity, UUID> {
}