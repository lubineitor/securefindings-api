package com.securefindings.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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

        private static final UUID ORGANIZATION_A = UUID.fromString(
                        "00000000-0000-0000-0000-000000000001");
        private static final UUID ORGANIZATION_B = UUID.fromString(
                        "00000000-0000-0000-0000-000000000002");

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
                                                requestId))
                                .andExpect(header().string(
                                                "X-RateLimit-Limit",
                                                "1"))
                                .andExpect(header().string(
                                                "X-RateLimit-Remaining",
                                                "0"))
                                .andExpect(header().exists(
                                                "X-RateLimit-Reset"));

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
                                                "X-RateLimit-Limit",
                                                "1"))
                                .andExpect(header().string(
                                                "X-RateLimit-Remaining",
                                                "0"))
                                .andExpect(header().exists(
                                                "X-RateLimit-Reset"))
                                .andExpect(header().string(
                                                RequestCorrelationFilter.HEADER_NAME,
                                                requestId))
                                .andExpect(jsonPath("$.code")
                                                .value("RATE_LIMIT_EXCEEDED"));
        }

        @Test
        void deberiaSepararLasCuotasJwtPorOrganizacionEnLaCadenaDeSeguridad()
                        throws Exception {

                mockMvc.perform(get("/api/v1/findings")
                                .with(jwtAutenticado(ORGANIZATION_A)))
                                .andExpect(status().isNotFound())
                                .andExpect(header().string(
                                                "X-RateLimit-Remaining",
                                                "0"));

                mockMvc.perform(get("/api/v1/findings")
                                .with(jwtAutenticado(ORGANIZATION_A)))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(jsonPath("$.code")
                                                .value("RATE_LIMIT_EXCEEDED"));

                mockMvc.perform(get("/api/v1/findings")
                                .with(jwtAutenticado(ORGANIZATION_B)))
                                .andExpect(status().isNotFound())
                                .andExpect(header().string(
                                                "X-RateLimit-Remaining",
                                                "0"));
        }

        @Test
        void noDebeAplicarElLimiteAlHealthCheck()
                        throws Exception {

                mockMvc.perform(get("/api/v1/health"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/v1/health"))
                                .andExpect(status().isOk());
        }

        private RequestPostProcessor jwtAutenticado(UUID organizationId) {
                return jwt()
                                .jwt(token -> token
                                                .subject("analista")
                                                .claim("preferred_username", "analista")
                                                .claim(
                                                                "organization_id",
                                                                organizationId.toString()))
                                .authorities(new SimpleGrantedAuthority(
                                                "ROLE_ANALYST"));
        }
}