package com.securefindings.organization.api;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.securefindings.api.error.GlobalExceptionHandler.ApiErrorResponse;
import com.securefindings.organization.application.OrganizationService;
import com.securefindings.organization.domain.Organization;

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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizaciones", description = "Administración global y operaciones de la organización actual")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

        private final OrganizationService organizationService;

        public OrganizationController(OrganizationService organizationService) {
                this.organizationService = organizationService;
        }

        @GetMapping
        @Operation(summary = "Listar organizaciones", description = "Devuelve una página de organizaciones. Solo ADMIN puede consultar el directorio global.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Organizaciones recuperadas correctamente", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationPageResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Los parámetros de paginación no son válidos", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "Solo ADMIN puede listar organizaciones"),
                        @ApiResponse(responseCode = "429", description = "Se ha superado el límite de peticiones", headers = {
                                        @Header(name = "Retry-After", description = "Segundos que deben transcurrir antes de reintentar", schema = @Schema(type = "integer", format = "int64")),
                                        @Header(name = "X-RateLimit-Limit", description = "Capacidad máxima del cubo de tokens", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Remaining", description = "Tokens enteros disponibles", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Reset", description = "Instante Unix en que el cubo volverá a estar lleno", schema = @Schema(type = "integer", format = "int64"))
                        }, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        public OrganizationPageResponse findOrganizations(
                        @Parameter(description = "Número de página. Empieza en 0", example = "0", in = ParameterIn.QUERY) @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,

                        @Parameter(description = "Número máximo de organizaciones por página", example = "20", in = ParameterIn.QUERY) @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

                Page<Organization> organizations = organizationService.findOrganizations(page, size);

                return OrganizationPageResponse.from(organizations);
        }

        @GetMapping("/{organizationId}")
        @Operation(summary = "Consultar una organización", description = "Devuelve una organización por identificador. Solo ADMIN puede consultar el directorio global.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Organización recuperada correctamente", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "El identificador no es un UUID válido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "Solo ADMIN puede consultar el directorio global"),
                        @ApiResponse(responseCode = "404", description = "La organización no existe", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "429", description = "Se ha superado el límite de peticiones", headers = {
                                        @Header(name = "Retry-After", description = "Segundos que deben transcurrir antes de reintentar", schema = @Schema(type = "integer", format = "int64")),
                                        @Header(name = "X-RateLimit-Limit", description = "Capacidad máxima del cubo de tokens", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Remaining", description = "Tokens enteros disponibles", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Reset", description = "Instante Unix en que el cubo volverá a estar lleno", schema = @Schema(type = "integer", format = "int64"))
                        }, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        public OrganizationResponse getOrganization(
                        @Parameter(description = "Identificador UUID de la organización", example = "00000000-0000-0000-0000-000000000001", in = ParameterIn.PATH) @PathVariable("organizationId") UUID organizationId) {

                return OrganizationResponse.from(
                                organizationService.getOrganization(organizationId));
        }

        @PatchMapping(value = "/{organizationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Actualizar una organización", description = "Actualiza el nombre visible de una organización por identificador. Solo ADMIN puede hacerlo; el ID, el slug y la fecha de creación se mantienen.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Organización actualizada correctamente", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "El identificador o el nombre no son válidos", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "Solo ADMIN puede actualizar organizaciones"),
                        @ApiResponse(responseCode = "404", description = "La organización no existe", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "429", description = "Se ha superado el límite de peticiones", headers = {
                                        @Header(name = "Retry-After", description = "Segundos que deben transcurrir antes de reintentar", schema = @Schema(type = "integer", format = "int64")),
                                        @Header(name = "X-RateLimit-Limit", description = "Capacidad máxima del cubo de tokens", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Remaining", description = "Tokens enteros disponibles", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Reset", description = "Instante Unix en que el cubo volverá a estar lleno", schema = @Schema(type = "integer", format = "int64"))
                        }, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        public OrganizationResponse updateOrganization(
                        @Parameter(description = "Identificador UUID de la organización", example = "00000000-0000-0000-0000-000000000001", in = ParameterIn.PATH) @PathVariable("organizationId") UUID organizationId,
                        @Valid @RequestBody UpdateOrganizationRequest request) {

                return OrganizationResponse.from(
                                organizationService.updateOrganization(
                                                organizationId,
                                                request.name()));
        }

        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Crear una organización", description = "Crea una organización con un nombre y un slug único. Solo ADMIN puede crear organizaciones.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Organización creada correctamente", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Los datos de la organización no son válidos", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "Solo ADMIN puede crear organizaciones"),
                        @ApiResponse(responseCode = "409", description = "El slug ya está en uso", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "429", description = "Se ha superado el límite de peticiones", headers = {
                                        @Header(name = "Retry-After", description = "Segundos que deben transcurrir antes de reintentar", schema = @Schema(type = "integer", format = "int64")),
                                        @Header(name = "X-RateLimit-Limit", description = "Capacidad máxima del cubo de tokens", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Remaining", description = "Tokens enteros disponibles", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Reset", description = "Instante Unix en que el cubo volverá a estar lleno", schema = @Schema(type = "integer", format = "int64"))
                        }, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        public ResponseEntity<OrganizationResponse> createOrganization(
                        @Valid @RequestBody CreateOrganizationRequest request) {

                Organization organization = organizationService.createOrganization(
                                request.name(),
                                request.slug());

                var location = ServletUriComponentsBuilder
                                .fromCurrentRequest()
                                .path("/{id}")
                                .buildAndExpand(organization.id())
                                .toUri();

                return ResponseEntity
                                .created(location)
                                .body(OrganizationResponse.from(organization));
        }

        @GetMapping("/current")
        @Operation(summary = "Consultar la organización actual", description = "Obtiene la organización asociada al claim organization_id del token")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Organización recuperada correctamente", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene acceso a la organización"),
                        @ApiResponse(responseCode = "429", description = "Se ha superado el límite de peticiones", headers = {
                                        @Header(name = "Retry-After", description = "Segundos que deben transcurrir antes de reintentar", schema = @Schema(type = "integer", format = "int64")),
                                        @Header(name = "X-RateLimit-Limit", description = "Capacidad máxima del cubo de tokens", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Remaining", description = "Tokens enteros disponibles", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Reset", description = "Instante Unix en que el cubo volverá a estar lleno", schema = @Schema(type = "integer", format = "int64"))
                        }, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        public OrganizationResponse getCurrentOrganization() {
                return OrganizationResponse.from(
                                organizationService.getCurrentOrganization());
        }

        @PatchMapping(value = "/current", consumes = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Actualizar el nombre de la organización actual", description = "Solo un ADMIN puede cambiar el nombre visible. El ID, el slug y la fecha de creación no cambian.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Organización actualizada correctamente", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "El nombre enviado no es válido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "Solo ADMIN puede actualizar la organización"),
                        @ApiResponse(responseCode = "429", description = "Se ha superado el límite de peticiones", headers = {
                                        @Header(name = "Retry-After", description = "Segundos que deben transcurrir antes de reintentar", schema = @Schema(type = "integer", format = "int64")),
                                        @Header(name = "X-RateLimit-Limit", description = "Capacidad máxima del cubo de tokens", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Remaining", description = "Tokens enteros disponibles", schema = @Schema(type = "integer", format = "int32")),
                                        @Header(name = "X-RateLimit-Reset", description = "Instante Unix en que el cubo volverá a estar lleno", schema = @Schema(type = "integer", format = "int64"))
                        }, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        public OrganizationResponse updateCurrentOrganizationName(
                        @Valid @RequestBody UpdateOrganizationRequest request) {

                return OrganizationResponse.from(
                                organizationService.updateCurrentOrganizationName(
                                                request.name()));
        }
}
