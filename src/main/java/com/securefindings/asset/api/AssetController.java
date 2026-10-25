package com.securefindings.asset.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.securefindings.asset.application.AssetService;
import com.securefindings.asset.domain.Asset;
import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.asset.domain.AssetType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Activos", description = "Inventario de activos por organización")
@SecurityRequirement(name = "bearerAuth")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    @Operation(summary = "Listar activos", description = "Devuelve los activos de la organización actual de forma paginada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Activos recuperados correctamente"),
            @ApiResponse(responseCode = "400", description = "Los filtros o parámetros no son válidos")
    })
    public AssetPageResponse findAll(
            @Parameter(description = "Número de página. Empieza en 0", in = ParameterIn.QUERY) @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Elementos por página. Entre 1 y 100", in = ParameterIn.QUERY) @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Busca por nombre o identificador", in = ParameterIn.QUERY) @RequestParam(name = "q", required = false) @Size(max = 100) String searchTerm,
            @RequestParam(name = "type", required = false) AssetType type,
            @RequestParam(name = "environment", required = false) AssetEnvironment environment,
            @RequestParam(name = "criticality", required = false) AssetCriticality criticality,
            @RequestParam(name = "status", required = false) AssetStatus status) {

        Page<Asset> assets = assetService.findPage(
                page,
                size,
                searchTerm,
                type,
                environment,
                criticality,
                status);

        return AssetPageResponse.from(assets);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un activo", description = "Busca un activo dentro de la organización actual")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Activo encontrado"),
            @ApiResponse(responseCode = "404", description = "El activo no existe en la organización actual")
    })
    public Asset getById(@PathVariable UUID id) {
        return assetService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Crear un activo", description = "Crea un activo en la organización actual")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Activo creado"),
            @ApiResponse(responseCode = "400", description = "Los datos enviados no son válidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe un activo con ese identificador")
    })
    public ResponseEntity<Asset> create(
            @Valid @RequestBody CreateAssetRequest request,
            UriComponentsBuilder uriBuilder) {

        Asset asset = assetService.create(
                request.name(),
                request.identifier(),
                request.type(),
                request.environment(),
                request.criticality());

        URI location = uriBuilder.path("/api/v1/assets/{id}")
                .buildAndExpand(asset.id())
                .toUri();

        return ResponseEntity.created(location).body(asset);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar un activo", description = "Actualiza los datos y el estado del activo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Activo actualizado"),
            @ApiResponse(responseCode = "400", description = "Los datos enviados no son válidos"),
            @ApiResponse(responseCode = "404", description = "El activo no existe en la organización actual"),
            @ApiResponse(responseCode = "409", description = "Ya existe un activo con ese identificador")
    })
    public Asset update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssetRequest request) {

        return assetService.update(
                id,
                request.name(),
                request.identifier(),
                request.type(),
                request.environment(),
                request.criticality(),
                request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un activo sin hallazgos", description = "Solo permite eliminar activos que no estén asociados a hallazgos")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Activo eliminado"),
            @ApiResponse(responseCode = "404", description = "El activo no existe en la organización actual"),
            @ApiResponse(responseCode = "409", description = "El activo está asociado a hallazgos")
    })
    public void delete(@PathVariable UUID id) {
        assetService.delete(id);
    }
}