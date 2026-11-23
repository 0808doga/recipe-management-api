package com.dogasimsek.recipeapi.dto;

import com.dogasimsek.recipeapi.entity.Ingredient;
import io.swagger.v3.oas.annotations.media.Schema;

public record IngredientResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Spaghetti") String name,
        @Schema(example = "200") Double quantity,
        @Schema(example = "g") String unit,
        @Schema(example = "1") Long recipeId,
        AuditInfo audit) {

    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getQuantity(),
                ingredient.getUnit(), ingredient.getRecipe().getId(), AuditInfo.from(ingredient));
    }
}
