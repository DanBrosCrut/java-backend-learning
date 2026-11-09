package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for {@code POST /api/cart/items} — a product position to add to the cart.
 *
 * @param productId the product ID in MoySklad
 * @param quantity  the quantity to add; adding the same product again increases it
 */
@Schema(description = "A product position to add to the file-based cart")
public record CartItemRequest(
        @Schema(description = "Product ID in MoySklad", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotBlank(message = "must not be blank")
        String productId,

        @Schema(description = "Quantity to add, at least 1", example = "2", minimum = "1")
        @Min(value = 1, message = "must be at least 1")
        int quantity) {
}
