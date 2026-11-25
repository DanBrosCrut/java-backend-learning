package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for {@code POST /api/orders} — the positions the client wants to order.
 * <p>
 * The API is stateless: the client (or an AI agent) owns the cart and sends the complete
 * list of positions for validation and creation.
 *
 * @param positions the ordered positions with product ID and quantity
 */
@Schema(description = "Order positions sent by the client; the backend is stateless")
public record CreateOrderRequest(
        @Schema(description = "Ordered positions; at least one is required")
        @NotEmpty(message = "must not be empty")
        List<@Valid Position> positions) {

    /**
     * A single ordered position.
     *
     * @param productId the product ID in MoySklad
     * @param quantity  the ordered quantity
     */
    @Schema(description = "An ordered position")
    public record Position(
            @Schema(description = "Product ID in MoySklad", example = "123e4567-e89b-12d3-a456-426614174000")
            @NotBlank(message = "must not be blank")
            String productId,

            @Schema(description = "Ordered quantity, at least 1", example = "2", minimum = "1")
            @Min(value = 1, message = "must be at least 1")
            int quantity) {
    }
}
