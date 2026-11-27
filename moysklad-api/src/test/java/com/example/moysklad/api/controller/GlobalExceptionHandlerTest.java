package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.ErrorResponse;
import com.example.moysklad.api.exception.InvalidCatalogOptionException;
import com.example.moysklad.api.exception.InvalidPaginationException;
import com.example.moysklad.api.exception.MoyskladApiException;
import com.example.moysklad.api.exception.OrderConfigurationException;
import com.example.moysklad.api.exception.OrderValidationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GlobalExceptionHandler}: every failure mode maps to a structured
 * {@link ErrorResponse} with the documented HTTP status, and internal exception details
 * never leak into the response.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturn400_whenPaginationIsInvalid() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidPaginationException(new InvalidPaginationException("page must be >= 0"));

        assertError(response, HttpStatus.BAD_REQUEST, "INVALID_PAGINATION", "page must be >= 0");
    }

    @Test
    void shouldReturn400_whenCatalogOptionIsInvalid() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidCatalogOptionException(new InvalidCatalogOptionException("unknown priceType"));

        assertError(response, HttpStatus.BAD_REQUEST, "INVALID_CATALOG_OPTION", "unknown priceType");
    }

    @Test
    void shouldReturn502_whenMoyskladApiFails() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMoyskladApiException(new MoyskladApiException("stock report failed with status 412"));

        assertError(response, HttpStatus.BAD_GATEWAY, "MOYSKLAD_ERROR", "stock report failed with status 412");
    }

    @Test
    void shouldReturn409_whenOrderValidationFails() {
        ResponseEntity<ErrorResponse> response = handler.handleOrderValidationFailedException(
                new OrderValidationFailedException("Order validation failed for 1 position(s): insufficient stock"));

        assertError(response, HttpStatus.CONFLICT, "ORDER_VALIDATION_FAILED", "insufficient stock");
    }

    @Test
    void shouldReturn500_whenOrderIsNotConfigured() {
        ResponseEntity<ErrorResponse> response = handler.handleOrderConfigurationException(
                new OrderConfigurationException("set moysklad.orders.organization-id"));

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_CONFIGURATION_ERROR", "organization-id");
    }

    @Test
    void shouldReturn400WithFieldMessages_whenRequestBodyFailsValidation() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("cartItemRequest", "quantity", "must be at least 1"),
                new FieldError("cartItemRequest", "productId", "must not be blank")));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message())
                .contains("quantity must be at least 1")
                .contains("productId must not be blank");
    }

    @Test
    void shouldReturn400_whenRequestBodyIsMalformed() {
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("unexpected token", (HttpInputMessage) null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(body -> {
                    assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
                    assertThat(body.message()).isEqualTo("Malformed or missing JSON request body");
                });
    }

    @Test
    void shouldReturn404_whenResourceIsMissing() {
        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFoundException(
                new NoResourceFoundException(HttpMethod.GET, "/favicon.ico", "No static resource /favicon.ico"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(body -> {
                    assertThat(body.code()).isEqualTo("NOT_FOUND");
                    assertThat(body.message()).contains("/favicon.ico");
                });
    }

    @Test
    void shouldReturn500WithGenericMessage_whenUnexpectedExceptionOccurs() {
        ResponseEntity<ErrorResponse> response =
                handler.handleException(new IllegalStateException("internal connection detail: secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(body -> {
                    assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
                    assertThat(body.message()).isEqualTo("An unexpected error occurred");
                    assertThat(body.message()).doesNotContain("secret");
                });
    }

    private static void assertError(ResponseEntity<ErrorResponse> response,
                                    HttpStatus status,
                                    String code,
                                    String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(code);
        assertThat(response.getBody().message()).contains(message);
    }
}
