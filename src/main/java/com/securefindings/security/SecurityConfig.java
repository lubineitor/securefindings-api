package com.securefindings.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter)
                        throws Exception {

                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/api/v1/health").permitAll()

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/v1/findings",
                                                                "/api/v1/findings/**")
                                                .hasAnyRole("ANALYST", "ADMIN")

                                                .requestMatchers(HttpMethod.POST, "/api/v1/findings")
                                                .hasAnyRole("ANALYST", "ADMIN")

                                                .requestMatchers(HttpMethod.PUT, "/api/v1/findings/**")
                                                .hasAnyRole("ANALYST", "ADMIN")

                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/findings/**")
                                                .hasAnyRole("ANALYST", "ADMIN")

                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/findings/**")
                                                .hasRole("ADMIN")

                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt
                                                                .jwtAuthenticationConverter(
                                                                                jwtAuthenticationConverter)))
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable());

                return http.build();
        }

        @Bean
        JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

                converter.setJwtGrantedAuthoritiesConverter(
                                new DelegatingJwtGrantedAuthoritiesConverter(
                                                new JwtGrantedAuthoritiesConverter(),
                                                new KeycloakRealmRoleConverter()));

                converter.setPrincipalClaimName("preferred_username");

                return converter;
        }
}