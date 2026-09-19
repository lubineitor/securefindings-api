package com.securefindings.audit.api;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;

public record FindingAuditResponse(
        @JsonProperty("id") UUID id,

        @JsonProperty("findingId") UUID findingId,

        @JsonProperty("action") AuditAction action,

        @JsonProperty("actor") String actor,

        @JsonProperty("occurredAt") Instant occurredAt,

        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("requestId") String requestId) {

    public static FindingAuditResponse from(
            FindingAuditEntity entity) {

        return new FindingAuditResponse(
                entity.getId(),
                entity.getFindingId(),
                entity.getAction(),
                entity.getActor(),
                entity.getOccurredAt(),
                entity.getRequestId());
    }
}