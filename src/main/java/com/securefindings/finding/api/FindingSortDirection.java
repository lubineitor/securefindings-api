package com.securefindings.finding.api;

import java.util.Arrays;

public enum FindingSortDirection {

    ASC,
    DESC;

    public static FindingSortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return DESC;
        }

        return Arrays.stream(values())
                .filter(direction -> direction.name()
                        .equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "La dirección de ordenación no está permitida: "
                                + value));
    }
}