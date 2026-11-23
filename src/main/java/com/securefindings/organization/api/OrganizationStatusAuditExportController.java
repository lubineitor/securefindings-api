package com.securefindings.organization.api;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.api.error.GlobalExceptionHandler.ApiErrorResponse;
import com.securefindings.organization.application.OrganizationStatusAuditQueryService;
import com.securefindings.organization.domain.OrganizationStatusAuditAction;
import com.securefindings.organization.persistence.OrganizationStatusAuditEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Auditoría de organizaciones")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationStatusAuditExportController {

    private final OrganizationStatusAuditQueryService queryService;
    private final OrganizationStatusAuditCsvExporter csvExporter;

    public OrganizationStatusAuditExportController(
            OrganizationStatusAuditQueryService queryService,
            OrganizationStatusAuditCsvExporter csvExporter) {
        this.queryService = queryService;
        this.csvExporter = csvExporter;
    }

    @GetMapping(value = "/{organizationId}/status-audit/export", produces = "text/csv")
    @Operation(summary = "Exportar la auditoría de estado de una organización", description = "Exporta los eventos filtrados en CSV. El máximo es de 1.000 eventos. Solo ADMIN puede usar este endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo CSV generado"),
            @ApiResponse(responseCode = "400", description = "Los filtros no son válidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "La exportación supera el límite de eventos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede exportar esta auditoría")
    })
    public ResponseEntity<byte[]> exportStatusAudit(
            @PathVariable("organizationId") UUID organizationId,
            @RequestParam(name = "action", required = false) OrganizationStatusAuditAction action,
            @RequestParam(name = "requestId", required = false) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}", message = "El identificador de petición no tiene un formato válido") String requestId,
            @RequestParam(name = "actor", required = false) @Size(max = 255, message = "El actor no puede superar los 255 caracteres") String actor,
            @RequestParam(name = "occurredFrom", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant occurredFrom,
            @RequestParam(name = "occurredTo", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant occurredTo) {

        List<OrganizationStatusAuditEntity> events = queryService.findForExport(
                organizationId,
                action,
                requestId,
                actor,
                occurredFrom,
                occurredTo);

        byte[] csv = csvExporter.toCsv(events)
                .getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"securefindings-organization-status-audit.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(csv);
    }
}