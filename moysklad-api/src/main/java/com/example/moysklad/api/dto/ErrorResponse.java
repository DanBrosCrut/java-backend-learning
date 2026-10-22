package com.example.moysklad.api.dto;

/**
 * Consistent error payload returned by the REST API.
 *
 * @param code    the machine-readable error code
 * @param message the human-readable error message
 */
public record ErrorResponse(String code, String message) {
}
