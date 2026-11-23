package com.dogasimsek.recipeapi.validation;

import com.dogasimsek.recipeapi.dto.IngredientRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class IngredientQuantityValidator implements ConstraintValidator<ValidIngredientQuantity, IngredientRequest> {

    private static final List<String> COUNTABLE_UNITS = List.of("piece", "slice", "clove");

    @Override
    public boolean isValid(IngredientRequest request, ConstraintValidatorContext context) {
        if (request == null || request.quantity() == null || request.unit() == null) {
            return true; // missing values are reported by @NotNull / @NotBlank
        }
        boolean countable = COUNTABLE_UNITS.contains(request.unit().trim().toLowerCase());
        boolean wholeNumber = request.quantity() % 1 == 0;
        if (!countable || wholeNumber) {
            return true;
        }
        // Attach the error to the "quantity" field so the client sees which field is wrong
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("quantity")
                .addConstraintViolation();
        return false;
    }
}
