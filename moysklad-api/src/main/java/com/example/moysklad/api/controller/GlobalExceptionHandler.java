package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.ErrorResponse;
import com.example.moysklad.api.exception.MoyskladApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Centralized exception handling for the REST API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
