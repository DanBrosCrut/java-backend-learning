package com.example.moysklad.api.exception;

/**
 * Thrown when communication with the MoySklad API fails.
 */
public class MoyskladApiException extends RuntimeException {

    /**
     * Creates the exception with an error message.
     *
     * @param message the error message
     */
    public MoyskladApiException(String message) {
        super(message);
    }

    /**
     * Creates the exception with an error message and underlying cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public MoyskladApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
