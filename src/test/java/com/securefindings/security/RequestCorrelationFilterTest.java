package com.securefindings.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class RequestCorrelationFilterTest {

    private RequestCorrelationFilter filter;

    @BeforeEach
    void configurarFiltro() {
        filter = new RequestCorrelationFilter();
        MDC.clear();
    }

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void deberiaGenerarUnIdentificadorSiNoSeRecibe() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> idEnLaCadena = new AtomicReference<>();

        FilterChain chain = (chainRequest, chainResponse) -> {
            idEnLaCadena.set(
                    (String) chainRequest.getAttribute(
                            RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE));

            assertEquals(
                    idEnLaCadena.get(),
                    MDC.get("requestId"));
        };

        filter.doFilter(request, response, chain);

        String responseId = response.getHeader(
                RequestCorrelationFilter.HEADER_NAME);

        assertNotNull(responseId);
        assertEquals(responseId, idEnLaCadena.get());
        assertTrue(responseId.matches(
                "[0-9a-fA-F-]{36}"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void deberiaConservarUnIdentificadorValido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
                RequestCorrelationFilter.HEADER_NAME,
                "request-123");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (chainRequest, chainResponse) -> {
                });

        assertEquals(
                "request-123",
                response.getHeader(
                        RequestCorrelationFilter.HEADER_NAME));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void deberiaReemplazarUnIdentificadorNoValido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
                RequestCorrelationFilter.HEADER_NAME,
                "id no valido\ncon salto");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (chainRequest, chainResponse) -> {
                });

        String responseId = response.getHeader(
                RequestCorrelationFilter.HEADER_NAME);

        assertNotNull(responseId);
        assertNotEquals(
                "id no valido\ncon salto",
                responseId);
        assertTrue(responseId.matches(
                "[0-9a-fA-F-]{36}"));
    }

    @Test
    void deberiaEliminarElAtributoDeLaPeticionAlFinalizar()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (chainRequest, chainResponse) -> {
                    assertNotNull(
                            chainRequest.getAttribute(
                                    RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE));
                });

        assertNull(
                request.getAttribute(
                        RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE));
    }
}