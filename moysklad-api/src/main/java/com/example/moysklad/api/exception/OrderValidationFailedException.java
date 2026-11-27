package com.example.moysklad.api.exception;

/**
 * Raised when an order cannot be created because one or more requested positions
 * are not fully covered by the current stock.
 */
public class OrderValidationFailedException extends RuntimeException {

    /**
     * Creates the exception with the aggregated validation message.
     *
     * @param message the human-readable summary of the failed positions
     */
    public OrderValidationFailedException(String message) {
        super(message);
    }
}
