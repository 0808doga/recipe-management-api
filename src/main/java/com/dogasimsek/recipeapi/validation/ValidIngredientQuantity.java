package com.dogasimsek.recipeapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom cross-field validation for an ingredient:
 * if the unit is "piece", "slice" or "clove", the quantity must be a whole number
 * (you cannot have 2.5 eggs). Placed on the class because it looks at two fields.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IngredientQuantityValidator.class)
public @interface ValidIngredientQuantity {

    String message() default "Quantity must be a whole number for countable units (piece, slice, clove)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
