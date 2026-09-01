package com.securefindings.api.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.securefindings.finding.application.FindingNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "Valor no válido"
                                : fieldError.getDefaultMessage(),
                        (firstMessage, ignoredMessage) -> firstMessage,
                        LinkedHashMap::new
                ));

        return new ApiErrorResponse(
                "VALIDATION_ERROR",
                "La petición contiene datos no válidos",
                errors
        );
    }

    @ExceptionHandler(FindingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleFindingNotFound(
            FindingNotFoundException exception
    ) {
        return new ApiErrorResponse(
                "FINDING_NOT_FOUND",
                exception.getMessage(),
                Map.of()
        );
    }

    public record ApiErrorResponse(
            String code,
            String message,
            Map<String, String> errors
    ) {
    }
}