package com.securefindings.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PrometheusEndpointStubController.class)
@Import(SecurityConfig.class)
class PrometheusScrapeAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void permiteElScrapingAlRolMetricsReader() throws Exception {
        mockMvc.perform(get(PrometheusEndpointStubController.ENDPOINT)
                .with(jwt().authorities(
                        new SimpleGrantedAuthority("ROLE_METRICS_READER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(PrometheusEndpointStubController.RESPONSE));
    }

    @Test
    void conservaElAccesoParaAdmin() throws Exception {
        mockMvc.perform(get(PrometheusEndpointStubController.ENDPOINT)
                .with(jwt().authorities(
                        new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(PrometheusEndpointStubController.RESPONSE));
    }

    @Test
    void rechazaElScrapingAlRolAnalyst() throws Exception {
        mockMvc.perform(get(PrometheusEndpointStubController.ENDPOINT)
                .with(jwt().authorities(
                        new SimpleGrantedAuthority("ROLE_ANALYST"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaElScrapingSinAutenticacion() throws Exception {
        mockMvc.perform(get(PrometheusEndpointStubController.ENDPOINT))
                .andExpect(status().isUnauthorized());
    }
}