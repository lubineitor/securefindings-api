package com.securefindings.audit.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.audit.application.AuditService;
import com.securefindings.audit.domain.AuditAction;
import com.securefindings.audit.persistence.FindingAuditEntity;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = FindingAuditController.class)
@Import({
                SecurityConfig.class,
                GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class FindingAuditControllerTest {

        private static final UUID ORGANIZATION_ID = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");

        @Autowired
        private WebApplicationContext context;

        @MockitoBean
        private AuditService auditService;

        private MockMvc mockMvc;

        @BeforeEach
        void configurarMockMvc() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @Test
        void deberiaDevolverElHistorialDeUnHallazgo()
                        throws Exception {

                UUID findingId = UUID.randomUUID();

                FindingAuditEntity createdEvent = new FindingAuditEntity(
                                UUID.randomUUID(),
                                findingId,
                                ORGANIZATION_ID,
                                AuditAction.CREATED,
                                "analista",
                                Instant.parse("2026-09-06T08:00:00Z"));

                FindingAuditEntity updatedEvent = new FindingAuditEntity(
                                UUID.randomUUID(),
                                findingId,
                                ORGANIZATION_ID,
                                AuditAction.UPDATED,
                                "analista",
                                Instant.parse("2026-09-06T08:05:00Z"));

                Page<FindingAuditEntity> auditPage = new PageImpl<>(
                                List.of(createdEvent, updatedEvent),
                                PageRequest.of(0, 20),
                                2);

                when(auditService.findPageByFindingId(
                                findingId,
                                0,
                                20))
                                .thenReturn(auditPage);

                mockMvc.perform(get(
                                "/api/v1/findings/{findingId}/audit",
                                findingId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.content[0].findingId")
                                                .value(findingId.toString()))
                                .andExpect(jsonPath("$.content[0].action")
                                                .value("CREATED"))
                                .andExpect(jsonPath("$.content[0].actor")
                                                .value("analista"))
                                .andExpect(jsonPath("$.content[1].action")
                                                .value("UPDATED"))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(20))
                                .andExpect(jsonPath("$.totalElements").value(2))
                                .andExpect(jsonPath("$.totalPages").value(1))
                                .andExpect(jsonPath("$.first").value(true))
                                .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        void deberiaDevolverUnaPaginaVaciaSiNoHayAuditoria()
                        throws Exception {

                UUID findingId = UUID.randomUUID();

                Page<FindingAuditEntity> emptyPage = new PageImpl<>(
                                List.of(),
                                PageRequest.of(0, 20),
                                0);

                when(auditService.findPageByFindingId(
                                findingId,
                                0,
                                20))
                                .thenReturn(emptyPage);

                mockMvc.perform(get(
                                "/api/v1/findings/{findingId}/audit",
                                findingId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content").isEmpty())
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(20))
                                .andExpect(jsonPath("$.totalElements").value(0))
                                .andExpect(jsonPath("$.totalPages").value(0))
                                .andExpect(jsonPath("$.first").value(true))
                                .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        void deberiaRechazarUnaPaginaNegativa()
                        throws Exception {

                UUID findingId = UUID.randomUUID();

                mockMvc.perform(get(
                                "/api/v1/findings/{findingId}/audit",
                                findingId)
                                .param("page", "-1"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"));

                verifyNoInteractions(auditService);
        }

        @Test
        void deberiaRechazarUnTamanoDePaginaCero()
                        throws Exception {

                UUID findingId = UUID.randomUUID();

                mockMvc.perform(get(
                                "/api/v1/findings/{findingId}/audit",
                                findingId)
                                .param("size", "0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"));

                verifyNoInteractions(auditService);
        }

        @Test
        void deberiaRechazarUnTamanoDePaginaSuperiorAlMaximo()
                        throws Exception {

                UUID findingId = UUID.randomUUID();

                mockMvc.perform(get(
                                "/api/v1/findings/{findingId}/audit",
                                findingId)
                                .param("size", "101"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"));

                verifyNoInteractions(auditService);
        }
}