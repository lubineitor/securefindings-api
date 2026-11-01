package com.securefindings.asset.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.domain.AssetAuditEvent;
import com.securefindings.asset.persistence.AssetAuditEntity;
import com.securefindings.asset.persistence.AssetAuditRepository;
import com.securefindings.security.OrganizationContext;
import com.securefindings.security.RequestCorrelationFilter;

@Service
public class AssetAuditService {

    private final AssetAuditRepository assetAuditRepository;
    private final OrganizationContext organizationContext;

    public AssetAuditService(
            AssetAuditRepository assetAuditRepository,
            OrganizationContext organizationContext) {
        this.assetAuditRepository = Objects.requireNonNull(assetAuditRepository);
        this.organizationContext = Objects.requireNonNull(organizationContext);
    }

    @Transactional
    public void register(UUID assetId, AssetAuditAction action) {
        UUID organizationId = organizationContext.currentOrganizationId();

        AssetAuditEvent event = new AssetAuditEvent(
                UUID.randomUUID(),
                assetId,
                organizationId,
                action,
                currentActor(),
                Instant.now(),
                RequestCorrelationFilter.currentRequestId());

        assetAuditRepository.save(new AssetAuditEntity(event));
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return "system";
        }

        return authentication.getName();
    }
}