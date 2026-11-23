package com.dogasimsek.recipeapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(description = "The refreshToken returned by /api/auth/login", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "Refresh token is required")
        String refreshToken) {
}
