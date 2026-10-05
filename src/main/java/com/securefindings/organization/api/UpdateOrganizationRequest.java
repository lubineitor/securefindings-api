package com.securefindings.organization.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @NotBlank(message = "El nombre de la organización es obligatorio") @Size(max = 150, message = "El nombre de la organización no puede superar 150 caracteres") String name) {
}