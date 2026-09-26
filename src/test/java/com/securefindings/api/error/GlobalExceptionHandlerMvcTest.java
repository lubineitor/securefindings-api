package com.securefindings.api.error;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.finding.api.FindingSortDirection;
import com.securefindings.finding.api.FindingSortField;
import com.securefindings.finding.application.FindingService;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = com.securefindings.finding.api.FindingController.class)
@Import({
                SecurityConfig.class,
                GlobalExceptionHandler.class
})
@WithMockUser(username = "analista", roles = "ANALYST")
class GlobalExceptionHandlerMvcTest {

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
        void deberiaDevolverUnErrorEstructuradoSiElJsonEsInvalido()
                        throws Exception {

                mockMvc.perform(post("/api/v1/findings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\": \"SQL Injection\","))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code")
                                                .value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.message")
                                                .value("La petición contiene datos no válidos"))
                                .andExpect(jsonPath("$.errors.body")
                                                .value("El cuerpo de la petición no tiene un formato válido"));

                verifyNoInteractions(findingService);
        }

        @Test
        void deberiaDevolver403SiLaOrganizacionNoEstaAutorizada()
                        throws Exception {

                when(findingService.findPage(
                                0,
                                20,
                                null,
                                null,
                                null,
                                FindingSortField.CREATED_AT,
                                FindingSortDirection.DESC))
                                .thenThrow(new AccessDeniedException(
                                                "La organización del token no está autorizada"));

                mockMvc.perform(get("/api/v1/findings"))
                                .andExpect(status().isForbidden())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.code")
                                                .value("FORBIDDEN"))
                                .andExpect(jsonPath("$.message")
                                                .value("El usuario no tiene permisos para acceder a este recurso"))
                                .andExpect(jsonPath("$.errors")
                                                .isEmpty());
        }
}
