package com.securefindings.asset.api;

import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetStatus;
import com.securefindings.asset.domain.AssetType;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAssetRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "El nombre del activo no puede estar vacío") @Size(max = 150, message = "El nombre del activo no puede superar los 150 caracteres") String name,

        @Pattern(regexp = "(?s).*\\S.*", message = "El identificador del activo no puede estar vacío") @Size(max = 255, message = "El identificador del activo no puede superar los 255 caracteres") String identifier,

        AssetType type,

        AssetEnvironment environment,

        AssetCriticality criticality,

        AssetStatus status) {
}