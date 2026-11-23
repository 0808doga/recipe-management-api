package com.dogasimsek.recipeapi.dto;

import com.dogasimsek.recipeapi.validation.ValidIngredientQuantity;
import com.dogasimsek.recipeapi.validation.ValidUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * @ValidIngredientQuantity is a custom cross-field check:
 * when the unit is "piece" the quantity must be a whole number.
 */
@ValidIngredientQuantity
public record IngredientRequest(
        @Schema(example = "Spaghetti")
        @NotBlank(message = "Ingredient name is required")
        @Size(max = 50, message = "Ingredient name cannot be longer than 50 characters")
        String name,

        @Schema(example = "200")
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        @DecimalMax(value = "10000", message = "Quantity cannot be more than 10000")
        Double quantity,

        @Schema(example = "g", description = "One of: g, kg, ml, l, tsp, tbsp, cup, piece, pinch, slice, clove")
        @NotBlank(message = "Unit is required")
        @ValidUnit
        String unit) {
}
