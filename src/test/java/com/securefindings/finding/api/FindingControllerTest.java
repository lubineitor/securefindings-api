package com.securefindings.finding.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.finding.application.FindingNotFoundException;
import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;
import com.securefindings.finding.domain.FindingStatus;

@WebMvcTest(FindingController.class)
@Import(GlobalExceptionHandler.class)
class FindingControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private FindingService findingService;

        @Test
        void deberiaDevolverUnaListaVacia() throws Exception {
                when(findingService.findAll())
                                .thenReturn(List.of());

                mockMvc.perform(get("/api/v1/findings"))
                                .andExpect(status().isOk())
                                .andExpect(content().json("[]"));
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
        void deberiaEliminarUnHallazgo() throws Exception {
                UUID id = UUID.randomUUID();

                mockMvc.perform(delete("/api/v1/findings/{id}", id))
                                .andExpect(status().isNoContent());
        }

        @Test
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
}