package com.securefindings.api.error;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.securefindings.finding.api.FindingController;
import com.securefindings.finding.application.FindingService;
import com.securefindings.security.SecurityConfig;

@WebMvcTest(controllers = FindingController.class)
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
}