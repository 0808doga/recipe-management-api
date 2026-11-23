package com.dogasimsek.recipeapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Returned after a successful login or token refresh. */
public record AuthResponse(
        @Schema(description = "JWT to send in the Authorization header", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Long-lived token used to get a new access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Access token lifetime in seconds", example = "3600") long expiresIn,
        UserResponse user) {
}
