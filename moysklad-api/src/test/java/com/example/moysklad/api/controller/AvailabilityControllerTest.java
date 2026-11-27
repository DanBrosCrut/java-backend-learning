package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.AvailabilityCheckResponse;
import com.example.moysklad.api.dto.AvailabilityCheckResponse.MissingPosition;
import com.example.moysklad.api.exception.MoyskladApiException;
import com.example.moysklad.api.service.AvailabilityService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP tests for {@link AvailabilityController}: the request body is passed through to the
 * service without bean validation, so the controller accepts empty, zero and negative
 * quantities and returns whatever the service computes.
 */
@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

    @Mock
    private AvailabilityService availabilityService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AvailabilityController(availabilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnAvailableTrue_whenPositionsAreEmpty() throws Exception {
        when(availabilityService.checkAvailability(any()))
                .thenReturn(new AvailabilityCheckResponse(true, List.of()));

        mockMvc.perform(post("/api/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.missing").isEmpty());
    }

    @Test
    void shouldReturnAvailableTrue_whenPositionsAreNull() throws Exception {
        when(availabilityService.checkAvailability(any()))
                .thenReturn(new AvailabilityCheckResponse(true, List.of()));

        mockMvc.perform(post("/api/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void shouldReturnAvailableTrue_whenQuantityIsZero() throws Exception {
        // Known gap (hardening candidate): no bean validation rejects quantity = 0.
        when(availabilityService.checkAvailability(any()))
                .thenReturn(new AvailabilityCheckResponse(true, List.of()));

        mockMvc.perform(post("/api/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"id\":\"00000000-0000-0000-0000-000000000000\",\"quantity\":0}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void shouldReturnAvailableTrue_whenQuantityIsNegative() throws Exception {
        // Known gap (hardening candidate): a negative quantity is silently treated as available.
        when(availabilityService.checkAvailability(any()))
                .thenReturn(new AvailabilityCheckResponse(true, List.of()));

        mockMvc.perform(post("/api/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"id\":\"00000000-0000-0000-0000-000000000000\",\"quantity\":-1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void shouldReturnMissingWithEmptyId_whenProductIdIsBlank() throws Exception {
        when(availabilityService.checkAvailability(any()))
                .thenReturn(new AvailabilityCheckResponse(false,
                        List.of(new MissingPosition("", 1, 0))));

        mockMvc.perform(post("/api/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"id\":\"\",\"quantity\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.missing[0].id").value(""))
                .andExpect(jsonPath("$.missing[0].available").value(0));
    }

    @Test
    void shouldReturnBadRequest_whenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnBadGateway_whenMoyskladApiFails() throws Exception {
        when(availabilityService.checkAvailability(any()))
                .thenThrow(new MoyskladApiException("upstream down"));

        mockMvc.perform(post("/api/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positions\":[{\"id\":\"00000000-0000-0000-0000-000000000000\",\"quantity\":1}]}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MOYSKLAD_ERROR"));
    }
}
