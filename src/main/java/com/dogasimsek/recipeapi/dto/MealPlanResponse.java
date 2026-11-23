package com.dogasimsek.recipeapi.dto;

import com.dogasimsek.recipeapi.entity.MealPlan;
import com.dogasimsek.recipeapi.entity.MealType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record MealPlanResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "2026-12-01") LocalDate date,
        @Schema(example = "DINNER") MealType mealType,
        @Schema(example = "1") Long recipeId,
        @Schema(example = "Spaghetti Carbonara") String recipeTitle,
        @Schema(example = "2") Long ownerId,
        AuditInfo audit) {

    public static MealPlanResponse from(MealPlan mealPlan) {
        return new MealPlanResponse(mealPlan.getId(), mealPlan.getDate(), mealPlan.getMealType(),
                mealPlan.getRecipe().getId(), mealPlan.getRecipe().getTitle(),
                mealPlan.getOwner().getId(), AuditInfo.from(mealPlan));
    }
}
