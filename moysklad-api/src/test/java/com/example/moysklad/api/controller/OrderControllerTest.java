package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.OrderValidationRequest;
import com.example.moysklad.api.dto.OrderValidationResponse;
import com.example.moysklad.api.service.OrderValidationService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP tests for {@link OrderController}.
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private static final String PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Mock
    private OrderValidationService orderValidationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(orderValidationService))
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
}
