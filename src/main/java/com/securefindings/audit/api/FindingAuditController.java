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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/findings/{findingId}/audit")
@Tag(name = "Auditoría", description = "Historial de operaciones de los hallazgos")
public class FindingAuditController {

        private final AuditService auditService;

        public FindingAuditController(AuditService auditService) {
                this.auditService = Objects.requireNonNull(auditService);
        }

        @GetMapping
        @Operation(summary = "Consultar el historial de un hallazgo", description = "Devuelve las operaciones registradas "
                        + "en orden cronológico")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Historial recuperado correctamente", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AuditResponse.class)))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos")
        })
        public List<AuditResponse> findByFindingId(
                        @Parameter(description = "Identificador del hallazgo", example = "3bfa1ad2-eee1-4ea5-ba7c-16b47d1da147", required = true, in = ParameterIn.PATH) @PathVariable("findingId") UUID findingId) {

                return auditService.findByFindingId(findingId)
                                .stream()
                                .map(entity -> {
                                        FindingAuditEntity nonNullEntity = Objects.requireNonNull(
                                                        entity,
                                                        "El repositorio devolvió "
                                                                        + "una auditoría nula");

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