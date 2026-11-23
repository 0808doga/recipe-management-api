package com.dogasimsek.recipeapi.dto;

import com.dogasimsek.recipeapi.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
        @Schema(example = "ADMIN")
        @NotNull(message = "Role is required (USER or ADMIN)")
        Role role) {
}
