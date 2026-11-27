package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.CreateOrderRequest;
import com.example.moysklad.api.dto.OrderResponse;
import com.example.moysklad.api.dto.OrderValidationRequest;
import com.example.moysklad.api.dto.OrderValidationResponse;
import com.example.moysklad.api.exception.MoyskladApiException;
import com.example.moysklad.api.exception.OrderValidationFailedException;
import com.example.moysklad.api.service.OrderService;
import com.example.moysklad.api.service.OrderValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP tests for {@link OrderController}: pre-order validation and customer order creation.
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private static final String PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Mock
    private OrderValidationService orderValidationService;

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(orderValidationService, orderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnValidationResult_whenRequestIsValid() throws Exception {
        when(orderValidationService.validate(any(OrderValidationRequest.class)))
                .thenReturn(new OrderValidationResponse(true, List.of()));

        mockMvc.perform(post("/api/orders/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"productId\":\"" + PRODUCT_ID + "\",\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(orderValidationService).validate(new OrderValidationRequest(
                List.of(new OrderValidationRequest.Position(PRODUCT_ID, 2))));
    }

    @Test
    void shouldReturnValidationError_whenPositionsAreEmpty() throws Exception {
        mockMvc.perform(post("/api/orders/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("positions must not be empty"));
    }

    @Test
    void shouldReturnValidationError_whenPositionQuantityIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/orders/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"productId\":\"" + PRODUCT_ID + "\",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("positions[0].quantity must be at least 1"));
    }

    @Test
    void shouldReturnValidationError_whenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/orders/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldCreateOrder_whenRequestIsValid() throws Exception {
        when(orderService.create(any(CreateOrderRequest.class)))
                .thenReturn(new OrderResponse("order-1", "00001", new BigDecimal("100.00")));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"productId\":\"" + PRODUCT_ID + "\",\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("order-1"))
                .andExpect(jsonPath("$.name").value("00001"))
                .andExpect(jsonPath("$.sum").value(100.00));

        verify(orderService).create(new CreateOrderRequest(
                List.of(new CreateOrderRequest.Position(PRODUCT_ID, 2))));
    }

    @Test
    void shouldReturnConflict_whenOrderValidationFails() throws Exception {
        when(orderService.create(any(CreateOrderRequest.class)))
                .thenThrow(new OrderValidationFailedException("Order validation failed for 1 position(s)"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"productId\":\"" + PRODUCT_ID + "\",\"quantity\":2}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_VALIDATION_FAILED"));
    }

    @Test
    void shouldReturnValidationError_whenOrderPositionsAreEmpty() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("positions must not be empty"));
    }

    @Test
    void shouldReturnValidationError_whenOrderQuantityIsNegative() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"productId\":\"" + PRODUCT_ID + "\",\"quantity\":-1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("positions[0].quantity must be at least 1"));
    }

    @Test
    void shouldReturnValidationError_whenOrderProductIdIsBlank() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"productId\":\"\",\"quantity\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("positions[0].productId must not be blank"));
    }

    @Test
    void shouldReturnValidationError_whenValidateQuantityIsNegative() throws Exception {
        mockMvc.perform(post("/api/orders/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"productId\":\"" + PRODUCT_ID + "\",\"quantity\":-1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("positions[0].quantity must be at least 1"));
    }

    @Test
    void shouldReturnValidationError_whenValidateProductIdIsBlank() throws Exception {
        mockMvc.perform(post("/api/orders/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"productId\":\"\",\"quantity\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("positions[0].productId must not be blank"));
    }

    @Test
    void shouldReturnValidationError_whenValidatePositionsAreNull() throws Exception {
        mockMvc.perform(post("/api/orders/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("positions must not be empty"));
    }

    @Test
    void shouldReturnBadGateway_whenValidateMoyskladApiFails() throws Exception {
        when(orderValidationService.validate(any(OrderValidationRequest.class)))
                .thenThrow(new MoyskladApiException("upstream down"));

        mockMvc.perform(post("/api/orders/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"productId\":\"" + PRODUCT_ID + "\",\"quantity\":2}]}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MOYSKLAD_ERROR"));
    }

    @Test
    void shouldReturnValidationError_whenOrderBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
