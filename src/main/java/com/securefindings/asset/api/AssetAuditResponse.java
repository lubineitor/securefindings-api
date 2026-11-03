package com.securefindings.asset.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.persistence.AssetAuditEntity;

public record AssetAuditResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("assetId") UUID assetId,
        @JsonProperty("action") AssetAuditAction action,
        @JsonProperty("actor") String actor,
        @JsonProperty("occurredAt") Instant occurredAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("requestId") String requestId) {

    public static AssetAuditResponse from(AssetAuditEntity entity) {
        AssetAuditEntity checkedEntity = Objects.requireNonNull(
                entity,
                "El repositorio devolvió un evento nulo");

        return new AssetAuditResponse(
                checkedEntity.getId(),
                checkedEntity.getAssetId(),
                checkedEntity.getAction(),
                checkedEntity.getActor(),
                checkedEntity.getOccurredAt(),
                checkedEntity.getRequestId());
    }
}