package com.securefindings.finding.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingTrend;
import com.securefindings.finding.domain.FindingTrendInterval;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = FindingController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class FindingTrendControllerTest {

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
    void deberiaDevolverLaTendenciaTemporalDeHallazgos()
            throws Exception {

        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");

        FindingTrend trend = new FindingTrend(
                from,
                to,
                FindingTrendInterval.DAY,
                List.of(new FindingTrend.Point(
                        from,
                        1L,
                        Map.of(
                                FindingSeverity.LOW, 0L,
                                FindingSeverity.MEDIUM, 0L,
                                FindingSeverity.HIGH, 1L,
                                FindingSeverity.CRITICAL, 0L))));

        when(findingService.getTrend(
                from,
                to,
                FindingTrendInterval.DAY,
                null))
                .thenReturn(trend);

        mockMvc.perform(get("/api/v1/findings/trends")
                .param("from", from.toString())
                .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interval").value("DAY"))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points[0].bucketStart")
                        .value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.points[0].total").value(1))
                .andExpect(jsonPath("$.points[0].bySeverity.HIGH").value(1))
                .andExpect(jsonPath("$.points[0].bySeverity.LOW").value(0));

        verify(findingService).getTrend(
                from,
                to,
                FindingTrendInterval.DAY,
                null);
    }
}