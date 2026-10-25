package com.securefindings.audit.api;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.api.error.GlobalExceptionHandler.ApiErrorResponse;
import com.securefindings.audit.application.AuditService;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/v1/organizations/current/audit")
@Tag(name = "Auditoría", description = "Historial de operaciones de la organización actual")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationAuditController {

    private final AuditService auditService;

    public OrganizationAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Consultar la auditoría de la organización actual", description = "Devuelve eventos paginados de la organización asociada al token. No permite seleccionar la organización mediante un parámetro del cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial recuperado correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FindingAuditPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Los filtros o parámetros no son válidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos"),
            @ApiResponse(responseCode = "429", description = "Se ha superado el límite de peticiones")
    })
    public FindingAuditPageResponse findOrganizationAudit(
            @Parameter(description = "Número de página. Empieza en 0", example = "0", in = ParameterIn.QUERY) @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Número máximo de eventos por página", example = "20", in = ParameterIn.QUERY) @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,

            @Parameter(description = "Filtrar por acción de auditoría", example = "UPDATED", in = ParameterIn.QUERY) @RequestParam(name = "action", required = false) AuditAction action,

            @Parameter(description = "Filtrar por identificador de petición", example = "audit-request-123", in = ParameterIn.QUERY) @RequestParam(name = "requestId", required = false) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}", message = "El identificador de petición no tiene un formato válido") String requestId,

            @Parameter(description = "Filtrar por actor responsable", example = "analista", in = ParameterIn.QUERY) @RequestParam(name = "actor", required = false) @Size(max = 255, message = "El actor no puede superar los 255 caracteres") String actor,

            @Parameter(description = "Fecha y hora inicial inclusive (ISO-8601)", example = "2026-10-01T00:00:00Z", in = ParameterIn.QUERY) @RequestParam(name = "occurredFrom", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant occurredFrom,

            @Parameter(description = "Fecha y hora final inclusive (ISO-8601)", example = "2026-10-19T23:59:59Z", in = ParameterIn.QUERY) @RequestParam(name = "occurredTo", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant occurredTo) {

        Page<FindingAuditEntity> auditPage = auditService.findPageByOrganization(
                page,
                size,
                action,
                requestId,
                actor,
                occurredFrom,
                occurredTo);

        return FindingAuditPageResponse.from(auditPage);
    }
}