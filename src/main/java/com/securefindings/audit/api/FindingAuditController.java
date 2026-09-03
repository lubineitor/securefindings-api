package com.securefindings.audit.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.audit.application.AuditService;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;

@RestController
@RequestMapping("/api/v1/findings/{findingId}/audit")
public class FindingAuditController {

    private final AuditService auditService;

    public FindingAuditController(AuditService auditService) {
        this.auditService = Objects.requireNonNull(auditService);
    }

    @GetMapping
    public List<AuditResponse> findByFindingId(
            @PathVariable("findingId") UUID findingId) {

        return auditService.findByFindingId(findingId)
                .stream()
                .map(entity -> {
                    FindingAuditEntity nonNullEntity = Objects.requireNonNull(
                            entity,
                            "El repositorio devolvió una auditoría nula");

                    return new AuditResponse(
                            nonNullEntity.getId(),
                            nonNullEntity.getFindingId(),
                            nonNullEntity.getAction(),
                            nonNullEntity.getActor(),
                            nonNullEntity.getOccurredAt());
                })
                .toList();
    }

    public record AuditResponse(
            UUID id,
            UUID findingId,
            AuditAction action,
            String actor,
            Instant occurredAt) {
    }
}