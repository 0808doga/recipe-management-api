package com.dogasimsek.recipeapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation: the ingredient unit must be one of the known units.
 * The real check is in ValidUnitValidator.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidUnitValidator.class)
public @interface ValidUnit {

    String message() default "Unit is not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
