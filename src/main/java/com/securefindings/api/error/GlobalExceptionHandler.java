package com.securefindings.api.error;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.securefindings.asset.application.AssetArchivedException;
import com.securefindings.asset.application.AssetHasFindingsException;
import com.securefindings.asset.application.AssetIdentifierAlreadyExistsException;
import com.securefindings.asset.application.AssetNotFoundException;
import com.securefindings.finding.application.FindingExportLimitExceededException;
import com.securefindings.finding.application.FindingNotFoundException;
import com.securefindings.finding.application.FindingStatusTransitionException;
import com.securefindings.organization.application.OrganizationHasAssociatedDataException;
import com.securefindings.organization.application.OrganizationNotFoundException;
import com.securefindings.organization.application.OrganizationSlugAlreadyExistsException;

import jakarta.validation.ConstraintViolationException;

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

        @ExceptionHandler(HttpMessageNotReadableException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ApiErrorResponse handleUnreadableMessage() {

                return new ApiErrorResponse(
                                "VALIDATION_ERROR",
                                "La petición contiene datos no válidos",
                                Map.of(
                                                "body",
                                                "El cuerpo de la petición no tiene un formato válido"));
        }

        @ExceptionHandler(AccessDeniedException.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public ApiErrorResponse handleAccessDenied() {

                return new ApiErrorResponse(
                                "FORBIDDEN",
                                "El usuario no tiene permisos para acceder a este recurso",
                                Map.of());
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

        @ExceptionHandler(IllegalArgumentException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ApiErrorResponse handleIllegalArgument(
                        IllegalArgumentException exception) {

                return new ApiErrorResponse(
                                "VALIDATION_ERROR",
                                "La petición contiene datos no válidos",
                                Map.of("parameter", exception.getMessage()));
        }

        public record ApiErrorResponse(
                        String code,
                        String message,
                        Map<String, String> errors) {
        }

        @ExceptionHandler(ConstraintViolationException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ApiErrorResponse handleConstraintViolation(
                        ConstraintViolationException exception) {

                Map<String, String> errors = exception
                                .getConstraintViolations()
                                .stream()
                                .collect(Collectors.toMap(
                                                violation -> extractFieldName(
                                                                violation.getPropertyPath().toString()),
                                                violation -> violation.getMessage() == null
                                                                ? "Valor no válido"
                                                                : violation.getMessage(),
                                                (firstMessage, ignoredMessage) -> firstMessage,
                                                LinkedHashMap::new));

                return new ApiErrorResponse(
                                "VALIDATION_ERROR",
                                "La petición contiene datos no válidos",
                                errors);
        }

        private String extractFieldName(String propertyPath) {
                int lastSeparator = propertyPath.lastIndexOf('.');

                if (lastSeparator < 0) {
                        return propertyPath;
                }

                return propertyPath.substring(lastSeparator + 1);
        }

        @ExceptionHandler(FindingStatusTransitionException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ApiErrorResponse handleFindingStatusTransition(
                        FindingStatusTransitionException exception) {

                return new ApiErrorResponse(
                                "INVALID_STATUS_TRANSITION",
                                exception.getMessage(),
                                Map.of());
        }

        @ExceptionHandler(FindingExportLimitExceededException.class)
        @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
        public ApiErrorResponse handleFindingExportLimitExceeded(
                        FindingExportLimitExceededException exception) {

                return new ApiErrorResponse(
                                "EXPORT_LIMIT_EXCEEDED",
                                exception.getMessage(),
                                Map.of("limit", String.valueOf(exception.maximumRows())));
        }

        @ExceptionHandler(OrganizationNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ApiErrorResponse handleOrganizationNotFound(
                        OrganizationNotFoundException exception) {

                return new ApiErrorResponse(
                                "ORGANIZATION_NOT_FOUND",
                                exception.getMessage(),
                                Map.of());
        }

        @ExceptionHandler(OrganizationSlugAlreadyExistsException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ApiErrorResponse handleOrganizationSlugAlreadyExists(
                        OrganizationSlugAlreadyExistsException exception) {

                return new ApiErrorResponse(
                                "ORGANIZATION_SLUG_ALREADY_EXISTS",
                                exception.getMessage(),
                                Map.of("slug", exception.slug()));
        }

        @ExceptionHandler(OrganizationHasAssociatedDataException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ApiErrorResponse handleOrganizationHasAssociatedData(
                        OrganizationHasAssociatedDataException exception) {

                return new ApiErrorResponse(
                                "ORGANIZATION_HAS_ASSOCIATED_DATA",
                                exception.getMessage(),
                                Map.of("organizationId", exception.organizationId().toString()));
        }

        @ExceptionHandler(AssetNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ApiErrorResponse handleAssetNotFound(AssetNotFoundException exception) {
                return new ApiErrorResponse(
                                "ASSET_NOT_FOUND",
                                exception.getMessage(),
                                Map.of("assetId", exception.assetId().toString()));
        }

        @ExceptionHandler(AssetIdentifierAlreadyExistsException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ApiErrorResponse handleAssetIdentifierAlreadyExists(
                        AssetIdentifierAlreadyExistsException exception) {
                return new ApiErrorResponse(
                                "ASSET_IDENTIFIER_ALREADY_EXISTS",
                                exception.getMessage(),
                                Map.of("identifier", exception.identifier()));
        }

        @ExceptionHandler(AssetHasFindingsException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ApiErrorResponse handleAssetHasFindings(AssetHasFindingsException exception) {
                return new ApiErrorResponse(
                                "ASSET_HAS_FINDINGS",
                                exception.getMessage(),
                                Map.of("assetId", exception.assetId().toString()));
        }

        @ExceptionHandler(AssetArchivedException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ApiErrorResponse handleAssetArchived(AssetArchivedException exception) {
                return new ApiErrorResponse(
                                "ASSET_ARCHIVED",
                                exception.getMessage(),
                                Map.of("assetId", exception.assetId().toString()));
        }
}
