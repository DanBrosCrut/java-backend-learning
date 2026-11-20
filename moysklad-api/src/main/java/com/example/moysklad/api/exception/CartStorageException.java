package com.example.moysklad.api.exception;

/**
 * Raised when the file-based cart cannot be read or written.
 */
public class CartStorageException extends RuntimeException {

    /**
     * Creates the exception with a message and the underlying I/O or serialization failure.
     *
     * @param message the description of the failed operation
     * @param cause   the underlying failure
     */
    public CartStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
