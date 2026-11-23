package com.dogasimsek.recipeapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

/**
 * Checks that the unit is one of ALLOWED_UNITS (case-insensitive).
 */
public class ValidUnitValidator implements ConstraintValidator<ValidUnit, String> {

    public static final List<String> ALLOWED_UNITS =
            List.of("g", "kg", "ml", "l", "tsp", "tbsp", "cup", "piece", "pinch", "slice", "clove");

    @Override
    public boolean isValid(String unit, ConstraintValidatorContext context) {
        // Empty values are checked by @NotBlank, not here
        if (unit == null || unit.isBlank()) {
            return true;
        }
        if (ALLOWED_UNITS.contains(unit.trim().toLowerCase())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Unit must be one of: " + String.join(", ", ALLOWED_UNITS))
                .addConstraintViolation();
        return false;
    }
}
