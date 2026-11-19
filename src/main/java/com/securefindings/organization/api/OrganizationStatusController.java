package com.securefindings.organization.api;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.securefindings.organization.application.OrganizationLifecycleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizaciones")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationStatusController {

    private final OrganizationLifecycleService lifecycleService;

    public OrganizationStatusController(
            OrganizationLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @PatchMapping(path = "/{organizationId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Suspender o reactivar una organización", description = "Solo ADMIN puede cambiar el estado global de una organización.")
    public OrganizationResponse changeStatus(
            @PathVariable UUID organizationId,
            @Valid @RequestBody UpdateOrganizationStatusRequest request) {

        return OrganizationResponse.from(
                lifecycleService.changeStatus(
                        organizationId,
                        request.status()));
    }
}