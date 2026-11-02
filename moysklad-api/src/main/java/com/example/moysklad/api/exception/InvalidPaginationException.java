package com.example.moysklad.api.exception;

/**
 * Raised when the pagination parameters of a catalog request are invalid.
 */
public class InvalidPaginationException extends RuntimeException {

    /**
     * Creates the exception with a message describing the invalid pagination parameter.
     *
     * @param message the human-readable reason
     */
    public InvalidPaginationException(String message) {
        super(message);
    }
}
