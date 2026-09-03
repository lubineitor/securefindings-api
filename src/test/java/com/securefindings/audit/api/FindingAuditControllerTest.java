package com.securefindings.audit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.audit.application.AuditService;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(FindingAuditController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "analista", roles = "ANALYST")
class FindingAuditControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deberiaDevolverElHistorialDeUnHallazgo() throws Exception {
        UUID findingId = UUID.randomUUID();

        when(auditService.findByFindingId(findingId))
                .thenReturn(List.of(
                        new FindingAuditEntity(
                                UUID.randomUUID(),
                                findingId,
                                AuditAction.CREATED,
                                "analista",
                                Instant.parse("2026-09-03T10:00:00Z")),
                        new FindingAuditEntity(
                                UUID.randomUUID(),
                                findingId,
                                AuditAction.UPDATED,
                                "analista",
                                Instant.parse("2026-09-03T10:05:00Z"))));

        mockMvc.perform(get(
                "/api/v1/findings/{findingId}/audit",
                findingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].findingId")
                        .value(findingId.toString()))
                .andExpect(jsonPath("$[0].action")
                        .value("CREATED"))
                .andExpect(jsonPath("$[0].actor")
                        .value("analista"))
                .andExpect(jsonPath("$[1].action")
                        .value("UPDATED"));
    }

    @Test
    void deberiaDevolverUnaListaVaciaSiNoHayAuditoria()
            throws Exception {

        UUID findingId = UUID.randomUUID();

        when(auditService.findByFindingId(findingId))
                .thenReturn(List.of());

        mockMvc.perform(get(
                "/api/v1/findings/{findingId}/audit",
                findingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}