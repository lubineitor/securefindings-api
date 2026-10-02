package com.securefindings.api.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.securefindings.api.error.GlobalExceptionHandler.ApiErrorResponse;
import com.securefindings.audit.api.FindingAuditController;
import com.securefindings.comment.api.FindingCommentController;
import com.securefindings.finding.api.FindingController;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

class RateLimitOpenApiDocumentationTest {

    @Test
    void todosLosEndpointsDeHallazgosDocumentanLaRespuesta429() {
        assertDocumentaRespuesta429(FindingController.class, "findAll");
        assertDocumentaRespuesta429(FindingController.class, "findById");
        assertDocumentaRespuesta429(FindingController.class, "create");
        assertDocumentaRespuesta429(FindingController.class, "updateStatus");
        assertDocumentaRespuesta429(FindingController.class, "update");
        assertDocumentaRespuesta429(FindingController.class, "delete");
    }

    @Test
    void todosLosEndpointsDeComentariosDocumentanLaRespuesta429() {
        assertDocumentaRespuesta429(FindingCommentController.class, "create");
        assertDocumentaRespuesta429(FindingCommentController.class, "findPage");
    }

    @Test
    void endpointDeAuditoriaDocumentaLaRespuesta429() {
        assertDocumentaRespuesta429(
                FindingAuditController.class,
                "findByFindingId");
    }

    private static void assertDocumentaRespuesta429(
            Class<?> controller,
            String methodName) {

        Method endpoint = Arrays.stream(controller.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No existe el endpoint "
                                + controller.getSimpleName()
                                + "."
                                + methodName));

        ApiResponse rateLimitResponse = Arrays.stream(
                endpoint.getAnnotationsByType(ApiResponse.class))
                .filter(response -> "429".equals(response.responseCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        controller.getSimpleName()
                                + "."
                                + methodName
                                + " no documenta la respuesta 429"));

        Content[] contents = rateLimitResponse.content();

        assertEquals(1, contents.length);
        assertEquals("application/json", contents[0].mediaType());
        assertEquals(
                ApiErrorResponse.class,
                contents[0].schema().implementation());
    }
}