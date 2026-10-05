package com.securefindings.organization.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.api.error.GlobalExceptionHandler.ApiErrorResponse;
import com.securefindings.organization.application.OrganizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizaciones", description = "Consulta de la organización asociada al usuario")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/current")
    @Operation(summary = "Consultar la organización actual", description = "Obtiene los datos de la organización asociada al token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organización recuperada correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrganizationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene acceso a la organización"),
            @ApiResponse(responseCode = "429", description = "Se ha superado el límite de peticiones", headers = {
                    @Header(name = "Retry-After", description = "Segundos que deben transcurrir antes de reintentar", schema = @Schema(type = "integer", format = "int64")),
                    @Header(name = "X-RateLimit-Limit", description = "Capacidad máxima del cubo de tokens", schema = @Schema(type = "integer", format = "int32")),
                    @Header(name = "X-RateLimit-Remaining", description = "Tokens enteros disponibles", schema = @Schema(type = "integer", format = "int32")),
                    @Header(name = "X-RateLimit-Reset", description = "Instante Unix en que el cubo volverá a estar lleno", schema = @Schema(type = "integer", format = "int64"))
            }, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public OrganizationResponse getCurrentOrganization() {
        return OrganizationResponse.from(
                organizationService.getCurrentOrganization());
    }
}