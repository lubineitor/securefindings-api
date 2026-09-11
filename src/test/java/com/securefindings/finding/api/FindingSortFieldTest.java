package com.securefindings.finding.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FindingSortFieldTest {

    @Test
    void deberiaUsarCreatedAtComoValorPorDefecto() {
        assertEquals(
                FindingSortField.CREATED_AT,
                FindingSortField.from(null));

        assertEquals(
                FindingSortField.CREATED_AT,
                FindingSortField.from(""));

        assertEquals(
                FindingSortField.CREATED_AT,
                FindingSortField.from("   "));
    }

    @Test
    void deberiaAceptarLosCamposPermitidosSinDistinguirMayusculas() {
        assertEquals(
                FindingSortField.CREATED_AT,
                FindingSortField.from("createdAt"));

        assertEquals(
                FindingSortField.UPDATED_AT,
                FindingSortField.from("UPDATEDAT"));

        assertEquals(
                FindingSortField.TITLE,
                FindingSortField.from("Title"));

        assertEquals(
                FindingSortField.SEVERITY,
                FindingSortField.from("severity"));

        assertEquals(
                FindingSortField.STATUS,
                FindingSortField.from("STATUS"));
    }

    @Test
    void deberiaAceptarTodosLosCamposDefinidos() {
        for (FindingSortField field : FindingSortField.values()) {
            assertEquals(
                    field,
                    FindingSortField.from(field.parameter()));
        }
    }

    @Test
    void deberiaRechazarUnCampoNoPermitido() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindingSortField.from("password"));

        assertEquals(
                "El campo de ordenación no está permitido: password",
                exception.getMessage());
    }
}