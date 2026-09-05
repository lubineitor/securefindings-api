package com.securefindings.finding.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.finding.application.FindingNotFoundException;
import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = FindingController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
@WithMockUser(username = "analista", roles = "ANALYST")
class FindingControllerTest {

        @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;

        @BeforeEach
        void configurarMockMvc() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @MockitoBean
        private FindingService findingService;

        @Test
        void deberiaDevolverUnaListaVacia() throws Exception {
                when(findingService.findPage(
                                0,
                                20,
                                null,
                                null))
                                .thenReturn(new PageImpl<>(
                                                List.of(),
                                                PageRequest.of(0, 20),
                                                0));

                mockMvc.perform(get("/api/v1/findings"))
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
        void deberiaCrearUnHallazgo() throws Exception {
                Finding finding = Finding.create(
                                "SQL Injection",
                                "Entrada de usuario sin validar",
                                FindingSeverity.HIGH);

                when(findingService.create(
                                "SQL Injection",
                                "Entrada de usuario sin validar",
                                FindingSeverity.HIGH))
                                .thenReturn(finding);

                mockMvc.perform(post("/api/v1/findings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "title": "SQL Injection",
                                                    "description": "Entrada de usuario sin validar",
                                                    "severity": "HIGH"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id")
                                                .value(finding.id().toString()))
                                .andExpect(jsonPath("$.title")
                                                .value("SQL Injection"))
                                .andExpect(jsonPath("$.description")
                                                .value("Entrada de usuario sin validar"))
                                .andExpect(jsonPath("$.severity")
                                                .value("HIGH"))
                                .andExpect(jsonPath("$.status")
                                                .value("OPEN"));
        }

        @Test
        void deberiaRechazarUnTituloVacio() throws Exception {
                mockMvc.perform(post("/api/v1/findings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "title": " ",
                                                    "description": "Descripción válida",
                                                    "severity": "HIGH"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.title")
                                                .value("El título es obligatorio"));
        }

        @Test
        void deberiaRechazarUnaPeticionSinSeveridad() throws Exception {
                mockMvc.perform(post("/api/v1/findings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "title": "SQL Injection",
                                                    "description": "Descripción válida"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.severity")
                                                .value("La severidad es obligatoria"));
        }

        @Test
        void deberiaObtenerUnHallazgoPorSuIdentificador() throws Exception {
                Finding finding = Finding.create(
                                "Cross-Site Scripting",
                                "Contenido no escapado correctamente",
                                FindingSeverity.MEDIUM);

                when(findingService.getById(finding.id()))
                                .thenReturn(finding);

                mockMvc.perform(get("/api/v1/findings/{id}", finding.id()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id")
                                                .value(finding.id().toString()))
                                .andExpect(jsonPath("$.title")
                                                .value("Cross-Site Scripting"))
                                .andExpect(jsonPath("$.severity")
                                                .value("MEDIUM"))
                                .andExpect(jsonPath("$.status")
                                                .value("OPEN"));
        }

        @Test
        void deberiaDevolver404SiElHallazgoNoExiste() throws Exception {
                UUID id = UUID.randomUUID();

                when(findingService.getById(id))
                                .thenThrow(new FindingNotFoundException(id));

                mockMvc.perform(get("/api/v1/findings/{id}", id))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code")
                                                .value("FINDING_NOT_FOUND"))
                                .andExpect(jsonPath("$.message")
                                                .value("No se ha encontrado el hallazgo con identificador: " + id));
        }

        @Test
        void deberiaActualizarElEstadoDeUnHallazgo() throws Exception {
                Finding finding = Finding.create(
                                "SQL Injection",
                                "Entrada de usuario sin validar",
                                FindingSeverity.HIGH);

                Finding updatedFinding = finding.withStatus(
                                FindingStatus.RESOLVED);

                when(findingService.updateStatus(
                                finding.id(),
                                FindingStatus.RESOLVED))
                                .thenReturn(updatedFinding);

                mockMvc.perform(patch("/api/v1/findings/{id}/status", finding.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "status": "RESOLVED"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id")
                                                .value(finding.id().toString()))
                                .andExpect(jsonPath("$.status")
                                                .value("RESOLVED"));
        }

        @Test
        void deberiaRechazarActualizacionSinEstado() throws Exception {
                UUID id = UUID.randomUUID();

                mockMvc.perform(patch("/api/v1/findings/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.status")
                                                .value("El estado es obligatorio"));
        }

        @Test
        void deberiaActualizarLosDatosDeUnHallazgo() throws Exception {
                Finding finding = Finding.create(
                                "SQL Injection",
                                "Entrada sin validar",
                                FindingSeverity.HIGH);

                Finding updatedFinding = finding.withDetails(
                                "SQL Injection corregida",
                                "Entrada validada correctamente",
                                FindingSeverity.CRITICAL);

                when(findingService.update(
                                finding.id(),
                                "SQL Injection corregida",
                                "Entrada validada correctamente",
                                FindingSeverity.CRITICAL))
                                .thenReturn(updatedFinding);

                mockMvc.perform(put("/api/v1/findings/{id}", finding.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "title": "SQL Injection corregida",
                                                    "description": "Entrada validada correctamente",
                                                    "severity": "CRITICAL"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id")
                                                .value(finding.id().toString()))
                                .andExpect(jsonPath("$.title")
                                                .value("SQL Injection corregida"))
                                .andExpect(jsonPath("$.description")
                                                .value("Entrada validada correctamente"))
                                .andExpect(jsonPath("$.severity")
                                                .value("CRITICAL"))
                                .andExpect(jsonPath("$.status")
                                                .value("OPEN"));
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaEliminarUnHallazgo() throws Exception {
                UUID id = UUID.randomUUID();

                mockMvc.perform(delete("/api/v1/findings/{id}", id))
                                .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "administrador", roles = "ADMIN")
        void deberiaDevolver404AlEliminarUnHallazgoInexistente()
                        throws Exception {
                UUID id = UUID.randomUUID();

                doThrow(new FindingNotFoundException(id))
                                .when(findingService)
                                .deleteById(id);

                mockMvc.perform(delete("/api/v1/findings/{id}", id))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code")
                                                .value("FINDING_NOT_FOUND"));
        }

        @Test
        void deberiaFiltrarLosHallazgosPorSeveridad() throws Exception {
                Finding finding = Finding.create(
                                "SQL Injection",
                                "Entrada sin validar",
                                FindingSeverity.HIGH);

                when(findingService.findPage(
                                0,
                                20,
                                FindingSeverity.HIGH,
                                null))
                                .thenReturn(new PageImpl<>(
                                                List.of(finding),
                                                PageRequest.of(0, 20),
                                                1));

                mockMvc.perform(get("/api/v1/findings")
                                .param("severity", "HIGH"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].severity")
                                                .value("HIGH"))
                                .andExpect(jsonPath("$.totalElements")
                                                .value(1));

                verify(findingService).findPage(
                                0,
                                20,
                                FindingSeverity.HIGH,
                                null);
        }

        @Test
        void deberiaFiltrarLosHallazgosPorEstado() throws Exception {
                Finding finding = Finding.create(
                                "Cross-Site Scripting",
                                "Contenido sin escapar",
                                FindingSeverity.MEDIUM)
                                .withStatus(FindingStatus.IN_PROGRESS);

                when(findingService.findPage(
                                0,
                                20,
                                null,
                                FindingStatus.IN_PROGRESS))
                                .thenReturn(new PageImpl<>(
                                                List.of(finding),
                                                PageRequest.of(0, 20),
                                                1));

                mockMvc.perform(get("/api/v1/findings")
                                .param("status", "IN_PROGRESS"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].status")
                                                .value("IN_PROGRESS"))
                                .andExpect(jsonPath("$.totalElements")
                                                .value(1));

                verify(findingService).findPage(
                                0,
                                20,
                                null,
                                FindingStatus.IN_PROGRESS);
        }

        @Test
        void deberiaFiltrarLosHallazgosPorSeveridadYEstado() throws Exception {
                Finding finding = Finding.create(
                                "Configuración insegura",
                                "Credenciales expuestas",
                                FindingSeverity.CRITICAL)
                                .withStatus(FindingStatus.RESOLVED);

                when(findingService.findPage(
                                0,
                                20,
                                FindingSeverity.CRITICAL,
                                FindingStatus.RESOLVED))
                                .thenReturn(new PageImpl<>(
                                                List.of(finding),
                                                PageRequest.of(0, 20),
                                                1));

                mockMvc.perform(get("/api/v1/findings")
                                .param("severity", "CRITICAL")
                                .param("status", "RESOLVED"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].severity")
                                                .value("CRITICAL"))
                                .andExpect(jsonPath("$.content[0].status")
                                                .value("RESOLVED"))
                                .andExpect(jsonPath("$.totalElements")
                                                .value(1));

                verify(findingService).findPage(
                                0,
                                20,
                                FindingSeverity.CRITICAL,
                                FindingStatus.RESOLVED);
        }
}