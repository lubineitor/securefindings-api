package com.securefindings.finding.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.finding.application.FindingService;
import com.securefindings.finding.domain.Finding;
import com.securefindings.finding.domain.FindingSeverity;

@WebMvcTest(FindingController.class)
@Import({ FindingService.class, GlobalExceptionHandler.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FindingControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private FindingService findingService;

        @Test
        void deberiaDevolverUnaListaVacia() throws Exception {
                mockMvc.perform(get("/api/v1/findings"))
                                .andExpect(status().isOk())
                                .andExpect(content().json("[]"));
        }

        @Test
        void deberiaCrearUnHallazgo() throws Exception {
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
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.title").value("SQL Injection"))
                                .andExpect(jsonPath("$.description")
                                                .value("Entrada de usuario sin validar"))
                                .andExpect(jsonPath("$.severity").value("HIGH"))
                                .andExpect(jsonPath("$.status").value("OPEN"));
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
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
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
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.severity")
                                                .value("La severidad es obligatoria"));
        }

        @Test
        void deberiaObtenerUnHallazgoPorSuIdentificador() throws Exception {
                Finding finding = findingService.create(
                                "Cross-Site Scripting",
                                "Contenido no escapado correctamente",
                                FindingSeverity.MEDIUM);

                mockMvc.perform(get("/api/v1/findings/{id}", finding.id()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(finding.id().toString()))
                                .andExpect(jsonPath("$.title")
                                                .value("Cross-Site Scripting"))
                                .andExpect(jsonPath("$.severity").value("MEDIUM"))
                                .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        void deberiaDevolver404SiElHallazgoNoExiste() throws Exception {
                UUID id = UUID.randomUUID();

                mockMvc.perform(get("/api/v1/findings/{id}", id))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("FINDING_NOT_FOUND"))
                                .andExpect(jsonPath("$.message")
                                                .value("No se ha encontrado el hallazgo con identificador: " + id));
        }

        @Test
        void deberiaActualizarElEstadoDeUnHallazgo() throws Exception {
                Finding finding = findingService.create(
                                "SQL Injection",
                                "Entrada de usuario sin validar",
                                FindingSeverity.HIGH);

                mockMvc.perform(patch("/api/v1/findings/{id}/status", finding.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "status": "RESOLVED"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(finding.id().toString()))
                                .andExpect(jsonPath("$.status").value("RESOLVED"));
        }

        @Test
        void deberiaRechazarActualizacionSinEstado() throws Exception {
                Finding finding = findingService.create(
                                "SQL Injection",
                                "Entrada de usuario sin validar",
                                FindingSeverity.HIGH);

                mockMvc.perform(patch("/api/v1/findings/{id}/status", finding.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors.status")
                                                .value("El estado es obligatorio"));
        }

        @Test
        void deberiaActualizarLosDatosDeUnHallazgo() throws Exception {
                Finding finding = findingService.create(
                                "SQL Injection",
                                "Entrada sin validar",
                                FindingSeverity.HIGH);

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
                                .andExpect(jsonPath("$.id").value(finding.id().toString()))
                                .andExpect(jsonPath("$.title")
                                                .value("SQL Injection corregida"))
                                .andExpect(jsonPath("$.description")
                                                .value("Entrada validada correctamente"))
                                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                                .andExpect(jsonPath("$.status").value("OPEN"));
        }
}