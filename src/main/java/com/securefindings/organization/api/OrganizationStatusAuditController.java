package com.securefindings.organization.api;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Auditoría de organizaciones")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationStatusAuditController {

    private final OrganizationStatusAuditQueryService queryService;

    public OrganizationStatusAuditController(
            OrganizationStatusAuditQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{organizationId}/status-audit")
    @Operation(summary = "Consultar cambios de estado de una organización", description = "Solo ADMIN puede consultar los eventos. Admite filtros y paginación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos recuperados correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationStatusAuditPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "UUID, filtros o parámetros de paginación no válidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede consultar esta auditoría")
    })
    public OrganizationStatusAuditPageResponse findStatusAudit(
            @PathVariable("organizationId") UUID organizationId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(name = "action", required = false) OrganizationStatusAuditAction action,
            @RequestParam(name = "requestId", required = false) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}", message = "El identificador de petición no tiene un formato válido") String requestId,
            @RequestParam(name = "actor", required = false) @Size(max = 255, message = "El actor no puede superar los 255 caracteres") String actor,
            @RequestParam(name = "occurredFrom", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant occurredFrom,
            @RequestParam(name = "occurredTo", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant occurredTo) {

        Page<OrganizationStatusAuditEntity> auditPage = queryService.findPageByOrganizationId(
                organizationId,
                page,
                size,
                action,
                requestId,
                actor,
                occurredFrom,
                occurredTo);

        return OrganizationStatusAuditPageResponse.from(auditPage);
    }
}