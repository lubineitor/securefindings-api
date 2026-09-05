package com.securefindings.finding.api;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/findings")
@Tag(name = "Hallazgos", description = "Operaciones para gestionar hallazgos de seguridad")
public class FindingController {

        private final FindingService findingService;

        public FindingController(FindingService findingService) {
                this.findingService = findingService;
        }

        @GetMapping
        @Operation(summary = "Listar hallazgos", description = "Devuelve una página de hallazgos "
                        + "con filtros opcionales")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Hallazgos recuperados correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FindingPageResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos")
        })
        public FindingPageResponse findAll(
                        @Parameter(description = "Número de página. Empieza en 0", example = "0", in = ParameterIn.QUERY) @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,

                        @Parameter(description = "Número máximo de elementos por página", example = "20", in = ParameterIn.QUERY) @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,

                        @Parameter(description = "Filtrar por severidad", example = "HIGH", in = ParameterIn.QUERY) @RequestParam(name = "severity", required = false) FindingSeverity severity,

                        @Parameter(description = "Filtrar por estado", example = "OPEN", in = ParameterIn.QUERY) @RequestParam(name = "status", required = false) FindingStatus status) {

                Page<Finding> findingPage = findingService.findPage(
                                page,
                                size,
                                severity,
                                status);

                return FindingPageResponse.from(findingPage);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Obtener un hallazgo", description = "Devuelve un hallazgo mediante su identificador")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Hallazgo encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Finding.class))),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos"),
                        @ApiResponse(responseCode = "404", description = "El hallazgo no existe")
        })
        public Finding findById(
                        @Parameter(description = "Identificador del hallazgo", example = "3bfa1ad2-eee1-4ea5-ba7c-16b47d1da147", required = true) @PathVariable UUID id) {

                return findingService.getById(id);
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Crear un hallazgo", description = "Crea un nuevo hallazgo de seguridad")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Hallazgo creado correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Finding.class))),
                        @ApiResponse(responseCode = "400", description = "Los datos enviados no son válidos"),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos")
        })
        public Finding create(
                        @Valid @RequestBody CreateFindingRequest request) {

                return findingService.create(
                                request.title(),
                                request.description(),
                                request.severity());
        }

        @PatchMapping("/{id}/status")
        @Operation(summary = "Actualizar el estado", description = "Cambia el estado de un hallazgo y registra "
                        + "la operación en auditoría")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Finding.class))),
                        @ApiResponse(responseCode = "400", description = "El estado enviado no es válido"),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos"),
                        @ApiResponse(responseCode = "404", description = "El hallazgo no existe")
        })
        public Finding updateStatus(
                        @Parameter(description = "Identificador del hallazgo", required = true) @PathVariable UUID id,
                        @Valid @RequestBody UpdateFindingStatusRequest request) {

                return findingService.updateStatus(
                                id,
                                request.status());
        }

        @PutMapping("/{id}")
        @Operation(summary = "Actualizar un hallazgo", description = "Actualiza los datos de un hallazgo y registra "
                        + "la operación en auditoría")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Hallazgo actualizado correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Finding.class))),
                        @ApiResponse(responseCode = "400", description = "Los datos enviados no son válidos"),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos"),
                        @ApiResponse(responseCode = "404", description = "El hallazgo no existe")
        })
        public Finding update(
                        @Parameter(description = "Identificador del hallazgo", required = true) @PathVariable UUID id,
                        @Valid @RequestBody UpdateFindingRequest request) {

                return findingService.update(
                                id,
                                request.title(),
                                request.description(),
                                request.severity());
        }

        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        @Operation(summary = "Eliminar un hallazgo", description = "Elimina un hallazgo y conserva su evento "
                        + "de auditoría")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Hallazgo eliminado correctamente"),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "Solo un usuario ADMIN puede eliminar"),
                        @ApiResponse(responseCode = "404", description = "El hallazgo no existe")
        })
        public void delete(
                        @Parameter(description = "Identificador del hallazgo", required = true) @PathVariable UUID id) {

                findingService.deleteById(id);
        }
}