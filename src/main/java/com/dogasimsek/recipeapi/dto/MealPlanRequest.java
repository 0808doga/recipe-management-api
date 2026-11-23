package com.dogasimsek.recipeapi.dto;

import com.dogasimsek.recipeapi.entity.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record MealPlanRequest(
        @Schema(description = "Date in yyyy-MM-dd format (today or later)", example = "2026-12-01")
        @NotNull(message = "Date is required")
        @FutureOrPresent(message = "Meal plan date cannot be in the past")
        LocalDate date,

        @Schema(example = "DINNER")
        @NotNull(message = "Meal type is required")
        MealType mealType,

        @Schema(example = "1")
        @NotNull(message = "Recipe id is required")
        @Positive(message = "Recipe id must be a positive number")
        Long recipeId) {
}
