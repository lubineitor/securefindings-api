package com.securefindings.audit.api;

import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.audit.application.AuditService;

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
        @Operation(summary = "Consultar el historial de auditoría", description = "Devuelve el historial paginado y ordenado "
                        + "cronológicamente")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Historial recuperado correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FindingAuditPageResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos"),
                        @ApiResponse(responseCode = "404", description = "El hallazgo no existe")
        })
        public FindingAuditPageResponse findByFindingId(
                        @Parameter(description = "Identificador del hallazgo", in = ParameterIn.PATH, required = true) @PathVariable("findingId") UUID findingId,

                        @Parameter(description = "Número de página. Empieza en 0", example = "0", in = ParameterIn.QUERY) @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,

                        @Parameter(description = "Número máximo de eventos por página", example = "20", in = ParameterIn.QUERY) @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

                return FindingAuditPageResponse.from(
                                auditService.findPageByFindingId(
                                                findingId,
                                                page,
                                                size));
        }
}