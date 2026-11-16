package com.securefindings.finding.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = FindingController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class FindingDueDateControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private FindingService findingService;

    @MockitoBean
    private FindingCsvExporter findingCsvExporter;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deberiaCrearUnHallazgoConFechaLimite() throws Exception {
        Instant dueAt = Instant.parse("2026-12-01T09:00:00Z");

        Finding finding = Finding.create(
                "SQL Injection",
                "Entrada de usuario sin validar",
                FindingSeverity.HIGH,
                null,
                dueAt);

        when(findingService.create(
                "SQL Injection",
                "Entrada de usuario sin validar",
                FindingSeverity.HIGH,
                null,
                dueAt))
                .thenReturn(finding);

        mockMvc.perform(post("/api/v1/findings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "SQL Injection",
                          "description": "Entrada de usuario sin validar",
                          "severity": "HIGH",
                          "dueAt": "2026-12-01T09:00:00Z"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dueAt").value(dueAt.toString()));

        verify(findingService).create(
                "SQL Injection",
                "Entrada de usuario sin validar",
                FindingSeverity.HIGH,
                null,
                dueAt);
    }

    @Test
    void deberiaActualizarLaFechaLimiteDeUnHallazgo() throws Exception {
        Instant dueAt = Instant.parse("2026-12-01T09:00:00Z");

        Finding finding = Finding.create(
                "SQL Injection",
                "Descripción actualizada",
                FindingSeverity.CRITICAL,
                null,
                dueAt);

        when(findingService.update(
                finding.id(),
                "SQL Injection",
                "Descripción actualizada",
                FindingSeverity.CRITICAL,
                dueAt))
                .thenReturn(finding);

        mockMvc.perform(put("/api/v1/findings/{id}", finding.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "SQL Injection",
                          "description": "Descripción actualizada",
                          "severity": "CRITICAL",
                          "dueAt": "2026-12-01T09:00:00Z"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueAt").value(dueAt.toString()));

        verify(findingService).update(
                finding.id(),
                "SQL Injection",
                "Descripción actualizada",
                FindingSeverity.CRITICAL,
                dueAt);
    }
}