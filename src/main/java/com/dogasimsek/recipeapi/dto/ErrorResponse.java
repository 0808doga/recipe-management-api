package com.dogasimsek.recipeapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The same JSON error format is used for EVERY error in the API.
 * validationErrors is only shown when there are field errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(example = "2026-11-25T10:15:30") LocalDateTime timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "Validation failed") String message,
        @Schema(example = "/api/recipes") String path,
        @Schema(example = "{\"title\": \"Title is required\"}") Map<String, String> validationErrors) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, null);
    }

    public static ErrorResponse withValidationErrors(String message, String path, Map<String, String> errors) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, errors);
    }
}
