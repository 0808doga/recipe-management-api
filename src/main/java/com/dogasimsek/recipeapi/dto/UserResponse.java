package com.dogasimsek.recipeapi.dto;

import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

/** User data that is safe to return (no password!). */
public record UserResponse(
        @Schema(example = "2") Long id,
        @Schema(example = "Doga Simsek") String name,
        @Schema(example = "doga@example.com") String email,
        @Schema(example = "USER") Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
