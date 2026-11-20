package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.ErrorResponse;
import com.example.moysklad.api.exception.CartItemNotFoundException;
import com.example.moysklad.api.exception.CartStorageException;
import com.example.moysklad.api.exception.InvalidCatalogOptionException;
import com.example.moysklad.api.exception.InvalidPaginationException;
import com.example.moysklad.api.exception.MoyskladApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Centralized exception handling for the REST API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles invalid pagination parameters.
     *
     * @param ex the exception describing the invalid parameter
     * @return a 400 response with a consistent error body
     */
    @ExceptionHandler(InvalidPaginationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaginationException(InvalidPaginationException ex) {
        log.warn("Invalid pagination: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_PAGINATION", ex.getMessage()));
    }

    /**
     * Handles catalog options that cannot be resolved.
     *
     * @param ex the exception describing the invalid option
     * @return a 400 response with a consistent error body
     */
    @ExceptionHandler(InvalidCatalogOptionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCatalogOptionException(InvalidCatalogOptionException ex) {
        log.warn("Invalid catalog option: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_CATALOG_OPTION", ex.getMessage()));
    }

    /**
     * Handles failures raised by the MoySklad integration.
     *
     * @param ex the exception
     * @return a 502 response with a consistent error body
     */
    @ExceptionHandler(MoyskladApiException.class)
    public ResponseEntity<ErrorResponse> handleMoyskladApiException(MoyskladApiException ex) {
        log.error("MoySklad API error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("MOYSKLAD_ERROR", ex.getMessage()));
    }

    /**
     * Handles HTTP errors returned by the MoySklad API.
     *
     * @param ex the exception carrying the upstream status code
     * @return a 502 response with a status-specific error code
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientErrorException(HttpClientErrorException ex) {
        String code = switch (ex.getStatusCode().value()) {
            case 401 -> "MOYSKLAD_UNAUTHORIZED";
            case 403 -> "MOYSKLAD_FORBIDDEN";
            default -> "MOYSKLAD_HTTP_ERROR";
        };
        log.error("MoySklad HTTP error {}: {}", ex.getStatusCode().value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(code, ex.getMessage()));
    }

    /**
     * Handles a cart position that does not exist.
     *
     * @param ex the exception carrying the missing product ID
     * @return a 404 response with a consistent error body
     */
    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCartItemNotFoundException(CartItemNotFoundException ex) {
        log.warn("Cart item not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("CART_ITEM_NOT_FOUND", ex.getMessage()));
    }

    /**
     * Handles failures of the file-based cart storage.
     *
     * @param ex the exception carrying the failed operation
     * @return a 500 response with a consistent error body
     */
    @ExceptionHandler(CartStorageException.class)
    public ResponseEntity<ErrorResponse> handleCartStorageException(CartStorageException ex) {
        log.error("Cart storage error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("CART_STORAGE_ERROR", "The cart could not be read or written"));
    }

    /**
     * Handles request bodies that fail bean validation.
     *
     * @param ex the exception carrying the field errors
     * @return a 400 response with a consistent error body
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " "
                        + Optional.ofNullable(error.getDefaultMessage()).orElse("is invalid"))
                .sorted()
                .collect(Collectors.joining("; "));
        String safeMessage = message.isBlank() ? "Validation failed" : message;
        log.warn("Request validation failed: {}", safeMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", safeMessage));
    }

    /**
     * Handles malformed or missing JSON request bodies.
     *
     * @param ex the exception
     * @return a 400 response with a consistent error body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", "Malformed or missing JSON request body"));
    }

    /**
     * Handles any uncaught exception.
     *
     * @param ex the exception
     * @return a 500 response with a generic error code
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", ex.getMessage()));
    }
}
