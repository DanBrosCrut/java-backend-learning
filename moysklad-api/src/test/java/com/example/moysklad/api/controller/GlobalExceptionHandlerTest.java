package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.ErrorResponse;
import com.example.moysklad.api.exception.CartItemNotFoundException;
import com.example.moysklad.api.exception.CartStorageException;
import com.example.moysklad.api.exception.InvalidCatalogOptionException;
import com.example.moysklad.api.exception.InvalidPaginationException;
import com.example.moysklad.api.exception.MoyskladApiException;
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
    void shouldReturn404_whenCartItemIsMissing() {
        ResponseEntity<ErrorResponse> response =
                handler.handleCartItemNotFoundException(new CartItemNotFoundException("product-1"));

        assertError(response, HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "product-1");
    }

    @Test
    void shouldReturn500WithStaticMessage_whenCartStorageFails() {
        ResponseEntity<ErrorResponse> response = handler.handleCartStorageException(
                new CartStorageException("disk full", new IllegalStateException("disk full")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .isNotNull()
                .satisfies(body -> {
                    assertThat(body.code()).isEqualTo("CART_STORAGE_ERROR");
                    assertThat(body.message()).isEqualTo("The cart could not be read or written");
                    assertThat(body.message()).doesNotContain("disk full");
                });
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
