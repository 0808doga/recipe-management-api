package com.dogasimsek.recipeapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Data needed to create or update a recipe.
 * "ingredients" is optional. On update: if it is sent, the old ingredient list is replaced;
 * if it is left out (null), the ingredients stay unchanged.
 */
public record RecipeRequest(
        @Schema(example = "Spaghetti Carbonara")
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
        String title,

        @Schema(example = "Classic Italian pasta with eggs and cheese")
        @Size(max = 500, message = "Description cannot be longer than 500 characters")
        String description,

        @Schema(example = "1. Boil the pasta. 2. Fry the bacon. 3. Mix eggs and cheese. 4. Combine everything.")
        @NotBlank(message = "Instructions are required")
        @Size(max = 5000, message = "Instructions cannot be longer than 5000 characters")
        String instructions,

        @Schema(description = "Preparation time in minutes", example = "25")
        @NotNull(message = "Preparation time is required")
        @Min(value = 1, message = "Preparation time must be at least 1 minute")
        @Max(value = 1440, message = "Preparation time cannot be more than 1440 minutes (24 hours)")
        Integer preparationTime,

        @Schema(example = "2")
        @NotNull(message = "Servings is required")
        @Min(value = 1, message = "Servings must be at least 1")
        @Max(value = 50, message = "Servings cannot be more than 50")
        Integer servings,

        @Valid
        @Size(max = 50, message = "A recipe cannot have more than 50 ingredients")
        List<IngredientRequest> ingredients) {
}
