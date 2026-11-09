package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Optional;

/**
 * Response DTO for {@code GET /api/cart} — the complete cart contents.
 *
 * @param items         the cart positions in insertion order
 * @param totalQuantity the sum of all position quantities
 */
@Schema(description = "The complete contents of the file-based cart")
public record CartView(
        @Schema(description = "Cart positions in insertion order")
        List<CartItemView> items,

        @Schema(description = "Sum of all position quantities", example = "3")
        int totalQuantity) {

    /**
     * Creates a cart view from the stored positions.
     *
     * @param items the stored cart positions; {@code null} is treated as an empty cart
     * @return the cart view with the computed total quantity
     */
    public static CartView of(List<CartItemView> items) {
        List<CartItemView> safeItems = Optional.ofNullable(items).orElseGet(List::of);
        int totalQuantity = safeItems.stream()
                .mapToInt(CartItemView::quantity)
                .sum();
        return new CartView(List.copyOf(safeItems), totalQuantity);
    }
}
