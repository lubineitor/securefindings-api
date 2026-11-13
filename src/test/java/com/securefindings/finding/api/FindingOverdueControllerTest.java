package com.securefindings.finding.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class FindingOverdueControllerTest {

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
    void deberiaDevolverUnaPaginaDeHallazgosVencidos() throws Exception {
        Instant dueAt = Instant.parse("2020-01-01T00:00:00Z");

        Finding finding = Finding.create(
                "SQL Injection",
                "Entrada de usuario sin validar",
                FindingSeverity.HIGH,
                null,
                dueAt);

        when(findingService.findOverduePage(0, 20))
                .thenReturn(new PageImpl<>(
                        List.of(finding),
                        PageRequest.of(0, 20),
                        1));

        mockMvc.perform(get("/api/v1/findings/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(finding.id().toString()))
                .andExpect(jsonPath("$.content[0].dueAt")
                        .value(dueAt.toString()));

        verify(findingService).findOverduePage(0, 20);
    }
}