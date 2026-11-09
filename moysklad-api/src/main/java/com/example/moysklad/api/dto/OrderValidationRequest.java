package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for {@code POST /api/orders/validate} — the positions to validate before placing an order.
 *
 * @param positions the requested positions with product ID and quantity
 */
@Schema(description = "Positions to validate before placing an order")
public record OrderValidationRequest(
        @Schema(description = "Requested positions; at least one is required")
        @NotEmpty(message = "must not be empty")
        List<@Valid Position> positions) {

    /**
     * A single position to validate.
     *
     * @param productId the product ID in MoySklad
     * @param quantity  the requested quantity
     */
    @Schema(description = "A requested order position")
    public record Position(
            @Schema(description = "Product ID in MoySklad", example = "123e4567-e89b-12d3-a456-426614174000")
            @NotBlank(message = "must not be blank")
            String productId,

            @Schema(description = "Requested quantity, at least 1", example = "2", minimum = "1")
            @Min(value = 1, message = "must be at least 1")
            int quantity) {
    }
}
