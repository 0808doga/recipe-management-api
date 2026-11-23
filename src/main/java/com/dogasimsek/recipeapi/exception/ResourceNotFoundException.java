package com.dogasimsek.recipeapi.exception;

/** Thrown when a record does not exist. Returned as 404 Not Found. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
