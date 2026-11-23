package com.securefindings.organization.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;

public record OrganizationStatusAuditResponse(
                UUID id,
                UUID organizationId,
                OrganizationStatusAuditAction action,
                String actor,
                Instant occurredAt,
                @JsonInclude(JsonInclude.Include.NON_NULL) String requestId,
                @JsonInclude(JsonInclude.Include.NON_NULL) String reason) {

        public static OrganizationStatusAuditResponse from(
                        OrganizationStatusAuditEntity entity) {

                OrganizationStatusAuditEntity checkedEntity = Objects.requireNonNull(
                                entity,
                                "El repositorio devolvió un evento nulo");

                return new OrganizationStatusAuditResponse(
                                checkedEntity.getId(),
                                checkedEntity.getOrganizationId(),
                                checkedEntity.getAction(),
                                checkedEntity.getActor(),
                                checkedEntity.getOccurredAt(),
                                checkedEntity.getRequestId(),
                                checkedEntity.getReason());
        }
}