package com.securefindings.finding.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.api.error.GlobalExceptionHandler;
import com.securefindings.finding.application.FindingCsvImportService;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = FindingCsvImportController.class)
@Import({
                SecurityConfig.class,
                GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class FindingCsvImportControllerTest {

        @Autowired
        private WebApplicationContext context;

        @MockitoBean
        private FindingCsvImportService importService;

        private MockMvc mockMvc;

        @BeforeEach
        void configureMockMvc() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @Test
        void deberiaImportarElCsvYDevolver201() throws Exception {
                UUID findingId = UUID.fromString(
                                "55555555-5555-5555-5555-555555555555");

                when(importService.importCsv(any(byte[].class)))
                                .thenReturn(new FindingCsvImportResponse(
                                                1,
                                                List.of(findingId),
                                                List.of()));

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "hallazgos.csv",
                                "text/csv",
                                """
                                                title,description,severity,assetId,dueAt
                                                Hallazgo,Descripción,HIGH,,
                                                """.getBytes(StandardCharsets.UTF_8));

                mockMvc.perform(multipart("/api/v1/findings/import")
                                .file(file))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.importedCount").value(1))
                                .andExpect(jsonPath("$.findingIds[0]")
                                                .value(findingId.toString()))
                                .andExpect(jsonPath("$.errors").isEmpty());

                verify(importService).importCsv(any(byte[].class));
        }

        @Test
        void deberiaDevolver422YLosErroresPorFila() throws Exception {
                when(importService.importCsv(any(byte[].class)))
                                .thenReturn(new FindingCsvImportResponse(
                                                0,
                                                List.of(),
                                                List.of(new FindingCsvImportResponse.RowError(
                                                                3,
                                                                "severity",
                                                                "Severidad no válida"))));

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "hallazgos.csv",
                                "text/csv",
                                "contenido".getBytes(StandardCharsets.UTF_8));

                mockMvc.perform(multipart("/api/v1/findings/import")
                                .file(file))
                                .andExpect(status().isUnprocessableContent())
                                .andExpect(jsonPath("$.importedCount").value(0))
                                .andExpect(jsonPath("$.errors[0].rowNumber").value(3))
                                .andExpect(jsonPath("$.errors[0].field").value("severity"));
        }

        @Test
        void deberiaDescargarLaPlantillaCsv() throws Exception {
                mockMvc.perform(get("/api/v1/findings/import/template"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.parseMediaType("text/csv;charset=UTF-8")))
                                .andExpect(content().string(
                                                "title,description,severity,assetId,dueAt\r\n"))
                                .andExpect(header().string(
                                                "Content-Disposition",
                                                org.hamcrest.Matchers.containsString(
                                                                "findings-import-template.csv")));
        }
}