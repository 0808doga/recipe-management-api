package com.dogasimsek.recipeapi.exception;

/** Thrown when a business rule is broken. Returned as 400 Bad Request. */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}
