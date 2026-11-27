package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.CreateOrderRequest;
import com.example.moysklad.api.dto.OrderResponse;
import com.example.moysklad.api.dto.OrderValidationRequest;
import com.example.moysklad.api.dto.OrderValidationResponse;
import com.example.moysklad.api.service.OrderService;
import com.example.moysklad.api.service.OrderValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing order validation and creation.
 * <p>
 * The API is stateless: the client owns the cart and sends the complete list of positions.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Pre-order validation and customer order creation in MoySklad")
public class OrderController {

    private final OrderValidationService orderValidationService;
    private final OrderService orderService;

    /**
     * Creates the controller with the order validation and creation services.
     *
     * @param orderValidationService the pre-order validation logic
     * @param orderService           the customer order creation logic
     */
    public OrderController(OrderValidationService orderValidationService, OrderService orderService) {
        this.orderValidationService = orderValidationService;
        this.orderService = orderService;
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

    /**
     * Validates the positions and creates a customer order in MoySklad.
     *
     * @param request the positions to order
     * @return the created order
     */
    @Operation(summary = "Create a customer order",
            description = "Validates the requested positions against current stock and creates a customer "
                    + "order in MoySklad. When any position is short, no order is created and the response "
                    + "is 409 ORDER_VALIDATION_FAILED.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }
}
