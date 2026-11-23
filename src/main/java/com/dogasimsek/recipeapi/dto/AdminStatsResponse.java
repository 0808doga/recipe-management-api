package com.dogasimsek.recipeapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminStatsResponse(
        @Schema(example = "5") long totalUsers,
        @Schema(example = "1") long totalAdmins,
        @Schema(example = "12") long totalRecipes,
        @Schema(example = "40") long totalIngredients,
        @Schema(example = "3") long totalMealPlans) {
}
