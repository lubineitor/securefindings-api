package com.securefindings.finding.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/findings")
@Tag(name = "Hallazgos")
@SecurityRequirement(name = "bearerAuth")
public class FindingBulkStatusController {

    private final FindingService findingService;

    public FindingBulkStatusController(FindingService findingService) {
        this.findingService = findingService;
    }

    @PatchMapping(path = "/status/bulk", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar en lote el estado de hallazgos", description = "Actualiza entre 1 y 100 hallazgos de forma atómica; acepta un motivo opcional de entre 10 y 500 caracteres.")
    public BulkFindingStatusResponse updateStatuses(
            @Valid @RequestBody UpdateFindingStatusesRequest request) {

        List<Finding> updatedFindings = request.reason() == null
                ? findingService.updateStatuses(
                        request.findingIds(),
                        request.status())
                : findingService.updateStatuses(
                        request.findingIds(),
                        request.status(),
                        request.reason());

        return BulkFindingStatusResponse.from(
                request.status(),
                updatedFindings);
    }
}