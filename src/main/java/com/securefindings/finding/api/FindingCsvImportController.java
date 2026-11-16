package com.securefindings.finding.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.securefindings.finding.application.FindingCsvImportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/findings")
@Tag(name = "Hallazgos")
@SecurityRequirement(name = "bearerAuth")
public class FindingCsvImportController {

        private static final byte[] CSV_TEMPLATE = ("title,description,severity,assetId,dueAt\r\n")
                        .getBytes(StandardCharsets.UTF_8);

        private final FindingCsvImportService importService;

        public FindingCsvImportController(
                        FindingCsvImportService importService) {
                this.importService = importService;
        }

        @GetMapping(value = "/import/template", produces = "text/csv")
        @Operation(summary = "Descargar la plantilla CSV de hallazgos", description = "Descarga la cabecera CSV aceptada por el endpoint de importación")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Plantilla CSV descargada"),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos")
        })
        public ResponseEntity<byte[]> downloadTemplate() {
                return ResponseEntity.ok()
                                .contentType(new MediaType(
                                                "text",
                                                "csv",
                                                StandardCharsets.UTF_8))
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"findings-import-template.csv\"")
                                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                                .body(CSV_TEMPLATE.clone());
        }

        @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Importar hallazgos desde CSV", description = "Permite validar el fichero con dryRun=true antes de crear los hallazgos")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Validación previa completada sin crear hallazgos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FindingCsvImportResponse.class))),
                        @ApiResponse(responseCode = "201", description = "Hallazgos importados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FindingCsvImportResponse.class))),
                        @ApiResponse(responseCode = "422", description = "El fichero contiene filas no válidas", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FindingCsvImportResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Petición multipart no válida"),
                        @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
                        @ApiResponse(responseCode = "403", description = "El usuario no tiene permisos"),
                        @ApiResponse(responseCode = "413", description = "El fichero supera los 5 MiB")
        })
        public ResponseEntity<FindingCsvImportResponse> importCsv(
                        @Parameter(description = "Fichero CSV UTF-8 de hasta 5 MiB y 1.000 filas", required = true) @RequestPart("file") MultipartFile file,

                        @Parameter(description = "Valida el fichero sin guardar hallazgos") @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun) {

                final byte[] csvBytes;

                try {
                        csvBytes = file.getBytes();
                } catch (IOException exception) {
                        throw new IllegalArgumentException(
                                        "No se pudo leer el fichero CSV",
                                        exception);
                }

                FindingCsvImportResponse response = importService.importCsv(
                                csvBytes,
                                dryRun);

                if (!response.errors().isEmpty()) {
                        return ResponseEntity
                                        .status(HttpStatus.UNPROCESSABLE_CONTENT)
                                        .body(response);
                }

                if (response.dryRun()) {
                        return ResponseEntity.ok(response);
                }

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }
}