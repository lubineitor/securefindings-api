package com.securefindings.finding.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateFindingAssetRequest(

        @NotNull(message = "El identificador del activo es obligatorio") UUID assetId) {
}