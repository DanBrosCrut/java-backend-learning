package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.OrderValidationRequest;
import com.example.moysklad.api.dto.OrderValidationResponse;
import com.example.moysklad.api.service.OrderValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing pre-order validation.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Pre-order validation against current stock; no order is created")
public class OrderController {

    private final OrderValidationService orderValidationService;

    /**
     * Creates the controller with the order validation service.
     *
     * @param orderValidationService the pre-order validation logic
     */
    public OrderController(OrderValidationService orderValidationService) {
        this.orderValidationService = orderValidationService;
    }

    /**
     * Validates the requested positions against the current stock.
     *
     * @param request the positions to validate
     * @return the validation result with structured errors and alternatives when stock is insufficient
     */
    @Operation(summary = "Validate an order",
            description = "Checks the requested positions against current stock without creating an order. "
                    + "When a position is short, the response contains the requested and available quantities, "
                    + "a message and up to three in-stock alternatives from the same group.")
    @PostMapping("/validate")
    public OrderValidationResponse validate(@Valid @RequestBody OrderValidationRequest request) {
        return orderValidationService.validate(request);
    }
}
