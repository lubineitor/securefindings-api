package com.securefindings.finding.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.FindingOverdueSummary;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = FindingController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class FindingOverdueSummaryControllerTest {

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
    void deberiaDevolverElResumenDeHallazgosVencidos() throws Exception {
        Instant asOf = Instant.parse("2026-11-13T15:00:00Z");

        FindingOverdueSummary summary = new FindingOverdueSummary(
                asOf,
                2L,
                Map.of(
                        FindingStatus.OPEN, 1L,
                        FindingStatus.IN_PROGRESS, 1L,
                        FindingStatus.RESOLVED, 0L,
                        FindingStatus.FALSE_POSITIVE, 0L),
                Map.of(
                        FindingSeverity.LOW, 0L,
                        FindingSeverity.MEDIUM, 1L,
                        FindingSeverity.HIGH, 1L,
                        FindingSeverity.CRITICAL, 0L));

        when(findingService.getOverdueSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/findings/overdue/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value(asOf.toString()))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.byStatus.OPEN").value(1))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.byStatus.RESOLVED").value(0))
                .andExpect(jsonPath("$.bySeverity.HIGH").value(1))
                .andExpect(jsonPath("$.bySeverity.CRITICAL").value(0));

        verify(findingService).getOverdueSummary();
    }
}