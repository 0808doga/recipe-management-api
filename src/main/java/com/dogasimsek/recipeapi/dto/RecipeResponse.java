package com.dogasimsek.recipeapi.dto;

import com.dogasimsek.recipeapi.entity.Recipe;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RecipeResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Spaghetti Carbonara") String title,
        @Schema(example = "Classic Italian pasta with eggs and cheese") String description,
        @Schema(example = "1. Boil the pasta. 2. Fry the bacon. 3. Mix eggs and cheese.") String instructions,
        @Schema(example = "25") Integer preparationTime,
        @Schema(example = "2") Integer servings,
        @Schema(example = "2") Long ownerId,
        @Schema(example = "Demo User") String ownerName,
        List<IngredientResponse> ingredients,
        AuditInfo audit) {

    public static RecipeResponse from(Recipe recipe) {
        List<IngredientResponse> ingredientResponses = recipe.getIngredients().stream()
                .map(IngredientResponse::from)
                .toList();
        return new RecipeResponse(recipe.getId(), recipe.getTitle(), recipe.getDescription(),
                recipe.getInstructions(), recipe.getPreparationTime(), recipe.getServings(),
                recipe.getOwner().getId(), recipe.getOwner().getName(), ingredientResponses,
                AuditInfo.from(recipe));
    }
}
