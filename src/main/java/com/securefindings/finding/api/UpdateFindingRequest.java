package com.securefindings.finding.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.securefindings.finding.domain.FindingSeverity;

public record UpdateFindingRequest(

        @NotBlank(message = "El título es obligatorio") @Size(max = 200, message = "El título no puede superar los 200 caracteres") String title,

        @NotBlank(message = "La descripción es obligatoria") @Size(max = 5000, message = "La descripción no puede superar los 5000 caracteres") String description,

        @NotNull(message = "La severidad es obligatoria") FindingSeverity severity) {
}