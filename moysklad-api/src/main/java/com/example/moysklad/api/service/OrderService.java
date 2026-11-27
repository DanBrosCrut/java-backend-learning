package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapCustomerOrderDto;
import com.example.moysklad.api.client.dto.RemapCustomerOrderResponseDto;
import com.example.moysklad.api.config.OrderProperties;
import com.example.moysklad.api.dto.CreateOrderRequest;
import com.example.moysklad.api.dto.OrderResponse;
import com.example.moysklad.api.dto.OrderValidationRequest;
import com.example.moysklad.api.dto.OrderValidationResponse;
import com.example.moysklad.api.exception.OrderConfigurationException;
import com.example.moysklad.api.exception.OrderValidationFailedException;
import com.example.moysklad.api.mapper.CustomerOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Creates customer orders in MoySklad from client positions.
 * <p>
 * The service is stateless: the client sends the complete list of positions. Before creation
 * the positions are validated against the current stock; when any position is short, no order
 * is created and an {@link OrderValidationFailedException} is thrown.
 */
@Service
public class OrderService {

    private final OrderValidationService orderValidationService;
    private final CustomerOrderMapper customerOrderMapper;
    private final AssortmentCache assortmentCache;
    private final MoyskladClient moyskladClient;
    private final OrderProperties orderProperties;

    /**
     * Creates the service with the validation service, the order mapper and the catalog caches.
     *
     * @param orderValidationService the pre-order validation logic
     * @param customerOrderMapper    the Remap payload mapper
     * @param assortmentCache        the in-memory assortment snapshot used for prices
     * @param moyskladClient         the client creating the order in MoySklad
     * @param orderProperties        the organization and agent configuration
     */
    public OrderService(OrderValidationService orderValidationService,
                        CustomerOrderMapper customerOrderMapper,
                        AssortmentCache assortmentCache,
                        MoyskladClient moyskladClient,
                        OrderProperties orderProperties) {
        this.orderValidationService = orderValidationService;
        this.customerOrderMapper = customerOrderMapper;
        this.assortmentCache = assortmentCache;
        this.moyskladClient = moyskladClient;
        this.orderProperties = orderProperties;
    }

    /**
     * Validates the positions and creates a customer order in MoySklad.
     *
     * @param request the ordered positions
     * @return the created order
     * @throws OrderValidationFailedException when any position is not covered by the current stock
     * @throws OrderConfigurationException    when the organization or agent is not configured
     */
    public OrderResponse create(CreateOrderRequest request) {
        OrderValidationResponse validation = orderValidationService.validate(toValidationRequest(request));
        if (!validation.valid()) {
            throw new OrderValidationFailedException(summary(validation));
        }
        if (orderProperties.organizationId().isBlank() || orderProperties.agentId().isBlank()) {
            throw new OrderConfigurationException(
                    "Order creation is not configured: set moysklad.orders.organization-id "
                            + "and moysklad.orders.agent-id");
        }

        List<RemapAssortmentRow> rows = Optional.ofNullable(assortmentCache.get(false).body())
                .map(RemapAssortmentResponse::rows)
                .orElseGet(List::of);
        Map<String, BigDecimal> prices = customerOrderMapper.toPriceMap(rows);
        RemapCustomerOrderDto remapOrder = customerOrderMapper.toRemap(
                request, rows, prices, orderProperties.organizationId(), orderProperties.agentId());
        RemapCustomerOrderResponseDto created = moyskladClient.createCustomerOrder(remapOrder);
        return customerOrderMapper.toResponse(created);
    }

    private static OrderValidationRequest toValidationRequest(CreateOrderRequest request) {
        List<OrderValidationRequest.Position> positions = request.positions().stream()
                .map(position -> new OrderValidationRequest.Position(position.productId(), position.quantity()))
                .toList();
        return new OrderValidationRequest(positions);
    }

    private static String summary(OrderValidationResponse validation) {
        String details = validation.errors().stream()
                .map(error -> error.productId() + ": " + error.message())
                .collect(Collectors.joining("; "));
        return "Order validation failed for " + validation.errors().size() + " position(s): " + details;
    }
}
