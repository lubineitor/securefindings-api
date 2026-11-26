package com.securefindings.finding.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = FindingBulkStatusController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class FindingBulkStatusControllerTest {

    private static final UUID FIRST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID SECOND_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private FindingService findingService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void analystPuedeActualizarElEstadoDeVariosHallazgos() throws Exception {
        List<UUID> ids = List.of(FIRST_ID, SECOND_ID);
        List<Finding> updatedFindings = List.of(
                finding(FIRST_ID, FindingStatus.IN_PROGRESS),
                finding(SECOND_ID, FindingStatus.IN_PROGRESS));

        when(findingService.updateStatuses(
                ids,
                FindingStatus.IN_PROGRESS))
                .thenReturn(updatedFindings);

        mockMvc.perform(patch("/api/v1/findings/status/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "findingIds": [
                            "11111111-1111-1111-1111-111111111111",
                            "22222222-2222-2222-2222-222222222222"
                          ],
                          "status": "IN_PROGRESS"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(2))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.findingIds[0]").value(FIRST_ID.toString()))
                .andExpect(jsonPath("$.findingIds[1]").value(SECOND_ID.toString()));

        verify(findingService).updateStatuses(
                ids,
                FindingStatus.IN_PROGRESS);
    }

    @Test
    void deberiaRechazarUnaListaVaciaAntesDeInvocarElServicio()
            throws Exception {

        mockMvc.perform(patch("/api/v1/findings/status/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "findingIds": [],
                          "status": "IN_PROGRESS"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(findingService);
    }

    private Finding finding(UUID id, FindingStatus status) {
        Instant createdAt = Instant.parse("2026-11-01T10:00:00Z");

        return new Finding(
                id,
                "Hallazgo " + id,
                "Descripción del hallazgo",
                FindingSeverity.HIGH,
                status,
                createdAt,
                createdAt,
                null,
                null);
    }
}