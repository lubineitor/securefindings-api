package com.securefindings.audit.api;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import io.swagger.v3.oas.annotations.headers.Header;
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
@RequestMapping("/api/v1/findings/{findingId}/audit")
@Tag(name = "Auditoría", description = "Historial de operaciones de los hallazgos")
@SecurityRequirement(name = "bearerAuth")
public class FindingAuditController {

        private final AuditService auditService;

        public FindingAuditController(AuditService auditService) {
                this.auditService = auditService;
        }

        @GetMapping
        @Operation(summary = "Consultar el historial de auditoría", description = "Devuelve el historial paginado y ordenado cronológicamente")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Historial recuperado correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FindingAuditPageResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos"),
                        @ApiResponse(responseCode = "404", description = "El hallazgo no existe"),
                        @ApiResponse(responseCode = "400", description = "Los filtros o parámetros no son válidos"),
                        @ApiResponse(responseCode = "429", description = "Se ha superado el límite de peticiones", headers = {
                                        @Header(name = "Retry-After", description = "Segundos que deben transcurrir antes de reintentar", schema = @Schema(type = "integer", format = "int64")),
                                        @Header(name = "X-RateLimit-Limit", description = "Capacidad máxima del cubo de tokens", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Remaining", description = "Tokens enteros disponibles", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Reset", description = "Instante Unix en que el cubo volverá a estar lleno", schema = @Schema(type = "integer", format = "int64"))
                        }, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        public FindingAuditPageResponse findByFindingId(
                        @Parameter(description = "Identificador del hallazgo", in = ParameterIn.PATH, required = true) @PathVariable("findingId") UUID findingId,

                        @Parameter(description = "Número de página. Empieza en 0", example = "0", in = ParameterIn.QUERY) @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,

                        @Parameter(description = "Número máximo de eventos por página", example = "20", in = ParameterIn.QUERY) @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,

                        @Parameter(description = "Filtrar por acción de auditoría", example = "UPDATED", in = ParameterIn.QUERY) @RequestParam(name = "action", required = false) AuditAction action,

                        @Parameter(description = "Filtrar por identificador de petición", example = "audit-request-123", in = ParameterIn.QUERY) @RequestParam(name = "requestId", required = false) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}", message = "El identificador de petición no tiene un formato válido") String requestId,

                        @RequestParam(name = "actor", required = false) @Size(max = 255, message = "El actor no puede superar los 255 caracteres") String actor) {

                Page<FindingAuditEntity> auditPage;

                if (action == null && requestId == null && actor == null) {
                        auditPage = auditService.findPageByFindingId(
                                        findingId,
                                        page,
                                        size);
                } else if (actor == null) {
                        auditPage = auditService.findPageByFindingId(
                                        findingId,
                                        page,
                                        size,
                                        action,
                                        requestId);
                } else {
                        auditPage = auditService.findPageByFindingId(
                                        findingId,
                                        page,
                                        size,
                                        action,
                                        requestId,
                                        actor);
                }

                return FindingAuditPageResponse.from(auditPage);
        }
}