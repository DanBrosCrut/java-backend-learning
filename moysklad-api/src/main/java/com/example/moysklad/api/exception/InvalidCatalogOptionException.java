package com.example.moysklad.api.exception;

/**
 * Raised when a catalog option cannot be resolved, e.g. an unknown field or price type.
 */
public class InvalidCatalogOptionException extends RuntimeException {

    /**
     * Creates the exception with a message describing the invalid option.
     *
     * @param message the human-readable reason
     */
    public InvalidCatalogOptionException(String message) {
        super(message);
    }
}
