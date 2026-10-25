package com.securefindings.asset.api;

import com.securefindings.asset.domain.AssetCriticality;
import com.securefindings.asset.domain.AssetEnvironment;
import com.securefindings.asset.domain.AssetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssetRequest(
        @NotBlank(message = "El nombre del activo es obligatorio") @Size(max = 150, message = "El nombre del activo no puede superar los 150 caracteres") String name,

        @NotBlank(message = "El identificador del activo es obligatorio") @Size(max = 255, message = "El identificador del activo no puede superar los 255 caracteres") String identifier,

        @NotNull(message = "El tipo del activo es obligatorio") AssetType type,

        @NotNull(message = "El entorno del activo es obligatorio") AssetEnvironment environment,

        @NotNull(message = "La criticidad del activo es obligatoria") AssetCriticality criticality) {
}