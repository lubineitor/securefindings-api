package com.securefindings.api.error;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.core.MethodParameter;

import com.securefindings.finding.application.FindingNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ApiErrorResponse handleValidation(
                        MethodArgumentNotValidException exception) {

                Map<String, String> errors = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(Collectors.toMap(
                                                fieldError -> fieldError.getField(),
                                                fieldError -> fieldError.getDefaultMessage() == null
                                                                ? "Valor no válido"
                                                                : fieldError.getDefaultMessage(),
                                                (firstMessage, ignoredMessage) -> firstMessage,
                                                LinkedHashMap::new));

                return new ApiErrorResponse(
                                "VALIDATION_ERROR",
                                "La petición contiene datos no válidos",
                                errors);
        }

        @ExceptionHandler(HandlerMethodValidationException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ApiErrorResponse handleMethodValidation(
                        HandlerMethodValidationException exception) {

                Map<String, String> errors = new LinkedHashMap<>();

                for (ParameterValidationResult result : exception
                                .getParameterValidationResults()) {

                        String parameterName = resolveParameterName(result);

                        String message = result.getResolvableErrors()
                                        .stream()
                                        .map(error -> error.getDefaultMessage())
                                        .filter(Objects::nonNull)
                                        .findFirst()
                                        .orElse("El valor del parámetro no es válido");

                        errors.put(parameterName, message);
                }

                return new ApiErrorResponse(
                                "VALIDATION_ERROR",
                                "La petición contiene parámetros no válidos",
                                errors);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ApiErrorResponse handleTypeMismatch(
                        MethodArgumentTypeMismatchException exception) {

                String parameterName = exception.getName() == null
                                ? "parameter"
                                : exception.getName();

                String message = buildTypeMismatchMessage(exception);

                return new ApiErrorResponse(
                                "VALIDATION_ERROR",
                                "La petición contiene parámetros no válidos",
                                Map.of(parameterName, message));
        }

        @ExceptionHandler(FindingNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ApiErrorResponse handleFindingNotFound(
                        FindingNotFoundException exception) {

                return new ApiErrorResponse(
                                "FINDING_NOT_FOUND",
                                exception.getMessage(),
                                Map.of());
        }

        private String resolveParameterName(
                        ParameterValidationResult result) {

                MethodParameter methodParameter = result.getMethodParameter();

                RequestParam requestParam = methodParameter
                                .getParameterAnnotation(RequestParam.class);

                if (requestParam != null) {
                        if (!requestParam.name().isBlank()) {
                                return requestParam.name();
                        }

                        if (!requestParam.value().isBlank()) {
                                return requestParam.value();
                        }
                }

                String parameterName = methodParameter.getParameterName();

                return parameterName == null
                                ? "parameter"
                                : parameterName;
        }

        private String buildTypeMismatchMessage(
                        MethodArgumentTypeMismatchException exception) {

                Class<?> requiredType = exception.getRequiredType();

                if (requiredType != null && requiredType.isEnum()) {
                        String acceptedValues = Arrays
                                        .stream(requiredType.getEnumConstants())
                                        .map(value -> String.valueOf(value))
                                        .collect(Collectors.joining(", "));

                        return "El valor debe ser uno de: " + acceptedValues;
                }

                return "El valor del parámetro no tiene un formato válido";
        }

        public record ApiErrorResponse(
                        String code,
                        String message,
                        Map<String, String> errors) {
        }
}