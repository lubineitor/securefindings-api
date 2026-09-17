package com.securefindings.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.securefindings.health.HealthController;

@WebMvcTest(controllers = HealthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
                "securefindings.rate-limit.max-requests=1",
                "securefindings.rate-limit.window=60s"
})
class RateLimitSecurityIntegrationTest {

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

        @Test
        void deberiaAplicarElLimiteEnLaCadenaDeSeguridad()
                        throws Exception {

                String requestId = "rate-limit-test-123";

                mockMvc.perform(get("/api/v1/findings")
                                .header(
                                                RequestCorrelationFilter.HEADER_NAME,
                                                requestId))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().string(
                                                RequestCorrelationFilter.HEADER_NAME,
                                                requestId));

                mockMvc.perform(get("/api/v1/findings")
                                .header(
                                                RequestCorrelationFilter.HEADER_NAME,
                                                requestId))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.APPLICATION_JSON))
                                .andExpect(header().string(
                                                "Retry-After",
                                                "60"))
                                .andExpect(header().string(
                                                RequestCorrelationFilter.HEADER_NAME,
                                                requestId))
                                .andExpect(jsonPath("$.code")
                                                .value("RATE_LIMIT_EXCEEDED"));
        }

        @Test
        void noDebeAplicarElLimiteAlHealthCheck()
                        throws Exception {

                mockMvc.perform(get("/api/v1/health"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/health"))
                                .andExpect(status().isOk());
        }
}