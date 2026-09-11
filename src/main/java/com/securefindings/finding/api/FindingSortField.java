package com.securefindings.finding.api;

import java.util.Arrays;

public enum FindingSortField {

    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    TITLE("title"),
    SEVERITY("severity"),
    STATUS("status");

    private final String parameter;

    FindingSortField(String parameter) {
        this.parameter = parameter;
    }

    public String parameter() {
        return parameter;
    }

    public static FindingSortField from(String value) {
        if (value == null || value.isBlank()) {
            return CREATED_AT;
        }

        return Arrays.stream(values())
                .filter(field -> field.parameter.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "El campo de ordenación no está permitido: " + value));
    }
}