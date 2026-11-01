package com.securefindings.asset.api;

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
import com.securefindings.asset.application.AssetAuditQueryService;
import com.securefindings.asset.domain.AssetAuditAction;
import com.securefindings.asset.persistence.AssetAuditEntity;

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
@RequestMapping("/api/v1/assets/{assetId}/audit")
@Tag(name = "Auditoría de activos", description = "Historial de operaciones de activos")
@SecurityRequirement(name = "bearerAuth")
public class AssetAuditController {

    private final AssetAuditQueryService assetAuditQueryService;

    public AssetAuditController(AssetAuditQueryService assetAuditQueryService) {
        this.assetAuditQueryService = assetAuditQueryService;
    }

    @GetMapping
    @Operation(summary = "Consultar el historial de un activo", description = "Devuelve los eventos del activo dentro de la organización del token. "
            + "El historial puede consultarse aunque el activo se haya eliminado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial recuperado correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AssetAuditPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Los filtros o parámetros no son válidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos"),
            @ApiResponse(responseCode = "429", description = "Se ha superado el límite de peticiones")
    })
    public AssetAuditPageResponse findAssetAudit(
            @Parameter(description = "UUID del activo", in = ParameterIn.PATH) @PathVariable UUID assetId,

            @Parameter(description = "Número de página. Empieza en 0", example = "0", in = ParameterIn.QUERY) @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Elementos por página. Entre 1 y 100", example = "20", in = ParameterIn.QUERY) @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,

            @Parameter(description = "Filtrar por acción", example = "ARCHIVED", in = ParameterIn.QUERY) @RequestParam(name = "action", required = false) AssetAuditAction action,

            @Parameter(description = "Filtrar por identificador de petición", example = "asset-request-123", in = ParameterIn.QUERY) @RequestParam(name = "requestId", required = false) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}", message = "El identificador de petición no tiene un formato válido") String requestId,

            @Parameter(description = "Filtrar por actor", example = "analista", in = ParameterIn.QUERY) @RequestParam(name = "actor", required = false) @Size(max = 255, message = "El actor no puede superar los 255 caracteres") String actor,

            @Parameter(description = "Fecha inicial inclusive en formato ISO-8601", in = ParameterIn.QUERY) @RequestParam(name = "occurredFrom", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant occurredFrom,

            @Parameter(description = "Fecha final inclusive en formato ISO-8601", in = ParameterIn.QUERY) @RequestParam(name = "occurredTo", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant occurredTo) {

        Page<AssetAuditEntity> auditPage = assetAuditQueryService.findPageByAssetId(
                assetId,
                page,
                size,
                action,
                requestId,
                actor,
                occurredFrom,
                occurredTo);

        return AssetAuditPageResponse.from(auditPage);
    }
}