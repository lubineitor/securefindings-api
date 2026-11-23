package com.securefindings.organization.api;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.api.error.GlobalExceptionHandler.ApiErrorResponse;
import com.securefindings.organization.application.OrganizationStatusAuditQueryService;
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
    @Operation(summary = "Consultar cambios de estado de una organización", description = "Devuelve los eventos de suspensión y reactivación. Solo ADMIN puede consultarlos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos recuperados correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationStatusAuditPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "El UUID o los parámetros de paginación no son válidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede consultar esta auditoría")
    })
    public OrganizationStatusAuditPageResponse findStatusAudit(
            @PathVariable("organizationId") UUID organizationId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

        Page<OrganizationStatusAuditEntity> auditPage = queryService.findPageByOrganizationId(
                organizationId,
                page,
                size);

        return OrganizationStatusAuditPageResponse.from(auditPage);
    }
}