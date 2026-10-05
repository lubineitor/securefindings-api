package com.securefindings.api.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        private static final String[] RATE_LIMIT_HEADERS = {
                        "Retry-After",
                        "X-RateLimit-Limit",
                        "X-RateLimit-Remaining",
                        "X-RateLimit-Reset"
        };

        @Test
        void endpointsDeHallazgosDocumentanLaRespuesta429() {
                assertDocumentaRespuesta429(FindingController.class, "findAll");
                assertDocumentaRespuesta429(FindingController.class, "findById");
                assertDocumentaRespuesta429(FindingController.class, "create");
                assertDocumentaRespuesta429(FindingController.class, "updateStatus");
                assertDocumentaRespuesta429(FindingController.class, "update");
                assertDocumentaRespuesta429(FindingController.class, "delete");
        }

        @Test
        void endpointsDeComentariosDocumentanLaRespuesta429() {
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

                ApiResponse response = Arrays.stream(
                                endpoint.getAnnotationsByType(ApiResponse.class))
                                .filter(apiResponse -> "429".equals(apiResponse.responseCode()))
                                .findFirst()
                                .orElseThrow(() -> new AssertionError(
                                                controller.getSimpleName()
                                                                + "."
                                                                + methodName
                                                                + " no documenta la respuesta 429"));

                Content[] contents = response.content();

                assertEquals(1, contents.length);
                assertEquals("application/json", contents[0].mediaType());
                assertEquals(
                                ApiErrorResponse.class,
                                contents[0].schema().implementation());

                for (String expectedHeader : RATE_LIMIT_HEADERS) {
                        boolean headerIsDocumented = Arrays.stream(response.headers())
                                        .anyMatch(header -> expectedHeader.equals(header.name()));

                        assertTrue(
                                        headerIsDocumented,
                                        controller.getSimpleName()
                                                        + "."
                                                        + methodName
                                                        + " no documenta la cabecera "
                                                        + expectedHeader);
                }
        }
}