package com.securefindings.audit.api;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.finding.domain.FindingStatus;

public record FindingAuditResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("findingId") UUID findingId,
        @JsonProperty("action") AuditAction action,
        @JsonProperty("actor") String actor,
        @JsonProperty("occurredAt") Instant occurredAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("requestId") String requestId,
        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("previousStatus") FindingStatus previousStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("newStatus") FindingStatus newStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("reason") String reason) {

    public FindingAuditResponse(
            UUID id,
            UUID findingId,
            AuditAction action,
            String actor,
            Instant occurredAt,
            String requestId,
            FindingStatus previousStatus,
            FindingStatus newStatus) {
        this(
                id,
                findingId,
                action,
                actor,
                occurredAt,
                requestId,
                previousStatus,
                newStatus,
                null);
    }

    public static FindingAuditResponse from(FindingAuditEntity entity) {
        return new FindingAuditResponse(
                entity.getId(),
                entity.getFindingId(),
                entity.getAction(),
                entity.getActor(),
                entity.getOccurredAt(),
                entity.getRequestId(),
                entity.getPreviousStatus(),
                entity.getNewStatus(),
                entity.getReason());
    }
}