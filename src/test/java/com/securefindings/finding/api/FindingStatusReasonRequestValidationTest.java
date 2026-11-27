package com.securefindings.finding.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.securefindings.finding.domain.FindingStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class FindingStatusReasonRequestValidationTest {

    private static final UUID FINDING_ID = UUID.fromString(
            "11111111-1111-1111-1111-111111111111");

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        validatorFactory.close();
    }

    @Test
    void deberiaPermitirOmitirElMotivo() {
        UpdateFindingStatusRequest request = new UpdateFindingStatusRequest(FindingStatus.IN_PROGRESS);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void deberiaNormalizarYAceptarDiezPuntosDeCodigoUnicode() {
        String reason = "😀".repeat(10);
        UpdateFindingStatusRequest request = new UpdateFindingStatusRequest(
                FindingStatus.IN_PROGRESS,
                "  " + reason + "  ");

        assertEquals(reason, request.reason());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void deberiaRechazarUnMotivoConMenosDeDiezPuntosDeCodigo() {
        UpdateFindingStatusRequest request = new UpdateFindingStatusRequest(
                FindingStatus.IN_PROGRESS,
                "Causa");

        Set<ConstraintViolation<UpdateFindingStatusRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("reason", violations.iterator().next()
                .getPropertyPath().toString());
    }

    @Test
    void deberiaRechazarUnMotivoDeMasDeQuinientosPuntosDeCodigoEnElLote() {
        UpdateFindingStatusesRequest request = new UpdateFindingStatusesRequest(
                List.of(FINDING_ID),
                FindingStatus.IN_PROGRESS,
                "x".repeat(501));

        Set<ConstraintViolation<UpdateFindingStatusesRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("reason", violations.iterator().next()
                .getPropertyPath().toString());
    }

    @Test
    void deberiaTratarUnMotivoEnBlancoComoOmitido() {
        UpdateFindingStatusRequest request = new UpdateFindingStatusRequest(
                FindingStatus.IN_PROGRESS,
                "     ");

        assertNull(request.reason());
        assertTrue(validator.validate(request).isEmpty());
    }
}