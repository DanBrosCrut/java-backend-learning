package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.CartItemRequest;
import com.example.moysklad.api.dto.CartItemView;
import com.example.moysklad.api.dto.CartView;
import com.example.moysklad.api.exception.CartItemNotFoundException;
import com.example.moysklad.api.exception.CartStorageException;
import com.example.moysklad.api.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP tests for {@link CartController}.
 */
@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private static final String PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Mock
    private CartService cartService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CartController(cartService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnCartAndTotalQuantity_whenCartIsRequested() throws Exception {
        when(cartService.getCart()).thenReturn(new CartView(
                List.of(new CartItemView(PRODUCT_ID, 2)), 2));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalQuantity").value(2));
    }

    @Test
    void shouldReturnStorageError_whenCartCannotBeRead() throws Exception {
        when(cartService.getCart())
                .thenThrow(new CartStorageException("boom", new java.io.IOException("disk failure")));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("CART_STORAGE_ERROR"))
                .andExpect(jsonPath("$.message").value("The cart could not be read or written"));
    }

    @Test
    void shouldReturnCreated_whenItemIsAdded() throws Exception {
        when(cartService.addItem(any(CartItemRequest.class)))
                .thenReturn(new CartItemView(PRODUCT_ID, 2));

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + PRODUCT_ID + "\",\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.quantity").value(2));

        verify(cartService).addItem(new CartItemRequest(PRODUCT_ID, 2));
    }

    @Test
    void shouldReturnValidationError_whenQuantityIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + PRODUCT_ID + "\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("quantity must be at least 1"));
    }

    @Test
    void shouldReturnValidationError_whenProductIdIsBlank() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\" \",\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("productId must not be blank"));
    }

    @Test
    void shouldReturnNoContent_whenItemIsRemoved() throws Exception {
        mockMvc.perform(delete("/api/cart/items/{productId}", PRODUCT_ID))
                .andExpect(status().isNoContent());

        verify(cartService).removeItem(PRODUCT_ID);
    }

    @Test
    void shouldReturnNotFound_whenItemToRemoveDoesNotExist() throws Exception {
        doThrow(new CartItemNotFoundException(PRODUCT_ID)).when(cartService).removeItem(PRODUCT_ID);

        mockMvc.perform(delete("/api/cart/items/{productId}", PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Cart item not found: " + PRODUCT_ID));
    }
}
