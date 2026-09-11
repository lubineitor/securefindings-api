package com.securefindings.finding.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FindingSortDirectionTest {

    @Test
    void deberiaUsarDescComoValorPorDefecto() {
        assertEquals(
                FindingSortDirection.DESC,
                FindingSortDirection.from(null));

        assertEquals(
                FindingSortDirection.DESC,
                FindingSortDirection.from(""));

        assertEquals(
                FindingSortDirection.DESC,
                FindingSortDirection.from("   "));
    }

    @Test
    void deberiaAceptarAscYDescSinDistinguirMayusculas() {
        assertEquals(
                FindingSortDirection.ASC,
                FindingSortDirection.from("asc"));

        assertEquals(
                FindingSortDirection.DESC,
                FindingSortDirection.from("desc"));
    }

    @Test
    void deberiaRechazarUnaDireccionNoPermitida() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FindingSortDirection.from("random"));

        assertEquals(
                "La dirección de ordenación no está permitida: random",
                exception.getMessage());
    }
}