package com.dogasimsek.recipeapi.exception;

/** Thrown when a record already exists (e.g. same e-mail). Returned as 409 Conflict. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
