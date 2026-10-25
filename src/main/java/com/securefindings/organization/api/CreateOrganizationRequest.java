package com.securefindings.organization.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank(message = "El nombre de la organización es obligatorio") @Size(max = 150, message = "El nombre de la organización no puede superar 150 caracteres") String name,

        @NotBlank(message = "El slug de la organización es obligatorio") @Size(max = 100, message = "El slug de la organización no puede superar 100 caracteres") @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "El slug solo puede contener letras minúsculas, números y guiones simples") String slug) {
}