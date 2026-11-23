package com.dogasimsek.recipeapi.dto;

import com.dogasimsek.recipeapi.entity.AuditableEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Audit Trail information shown in responses: who/when created and last changed a record. */
public record AuditInfo(
        @Schema(example = "2026-11-25T10:15:30") LocalDateTime createdAt,
        @Schema(example = "user@recipes.local") String createdBy,
        @Schema(example = "2026-11-25T10:20:00") LocalDateTime updatedAt,
        @Schema(example = "user@recipes.local") String lastModifiedBy) {

    public static AuditInfo from(AuditableEntity entity) {
        return new AuditInfo(entity.getCreatedAt(), entity.getCreatedBy(),
                entity.getUpdatedAt(), entity.getLastModifiedBy());
    }
}
