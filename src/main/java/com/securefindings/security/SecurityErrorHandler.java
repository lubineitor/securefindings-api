package com.securefindings.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SecurityErrorHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {

        writeError(
                response,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "La autenticación es necesaria para acceder a este recurso");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception)
            throws IOException {

        writeError(
                response,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "El usuario no tiene permisos para acceder a este recurso");
    }

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message)
            throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");

        response.getWriter().write("""
                {
                    "code": "%s",
                    "message": "%s",
                    "errors": {}
                }
                """.formatted(code, message));
    }
}