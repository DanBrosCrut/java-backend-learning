package com.example.moysklad.api.exception;

/**
 * Raised when order creation is requested but the required MoySklad references
 * (organization and agent) are not configured.
 */
public class OrderConfigurationException extends RuntimeException {

    /**
     * Creates the exception describing the missing configuration.
     *
     * @param message the human-readable explanation
     */
    public OrderConfigurationException(String message) {
        super(message);
    }
}
