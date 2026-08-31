package com.securefindings.finding.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import com.securefindings.finding.application.FindingService;

@WebMvcTest(FindingController.class)
@Import(FindingService.class)
class FindingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deberiaDevolverUnaListaVacia() throws Exception {
        mockMvc.perform(get("/api/v1/findings"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}