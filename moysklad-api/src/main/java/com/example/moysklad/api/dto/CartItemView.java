package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A single cart position as stored in the cart file and returned by the cart API.
 *
 * @param productId the product ID in MoySklad
 * @param quantity  the quantity in the cart
 */
@Schema(description = "A product position stored in the cart")
public record CartItemView(
        @Schema(description = "Product ID in MoySklad", example = "123e4567-e89b-12d3-a456-426614174000")
        String productId,

        @Schema(description = "Quantity in the cart", example = "2", minimum = "1")
        int quantity) {
}
