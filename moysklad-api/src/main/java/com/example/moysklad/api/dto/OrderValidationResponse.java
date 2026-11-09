package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for {@code POST /api/orders/validate} — the pre-order availability result.
 * <p>
 * Validation never creates an order. When {@code valid} is {@code false}, every missing position is
 * reported with the requested and available quantities and up to three in-stock alternatives.
 *
 * @param valid  whether every requested position is fully covered by the current stock
 * @param errors the positions that failed validation; empty when {@code valid} is true
 */
@Schema(description = "Pre-order availability result; validation never creates an order")
public record OrderValidationResponse(
        @Schema(description = "Whether every requested position is fully covered by the current stock")
        boolean valid,

        @Schema(description = "Validation errors for positions with insufficient stock")
        List<ValidationError> errors) {

    /**
     * A requested position that could not be fully covered by the current stock.
     *
     * @param productId    the product ID in MoySklad
     * @param requested    the requested quantity
     * @param available    the quantity currently in stock
     * @param message      a human-readable explanation
     * @param alternatives in-stock products suggested as replacements, at most three
     */
    @Schema(description = "A position with insufficient stock")
    public record ValidationError(
            @Schema(description = "Product ID in MoySklad", example = "123e4567-e89b-12d3-a456-426614174000")
            String productId,

            @Schema(description = "Requested quantity", example = "10")
            int requested,

            @Schema(description = "Quantity currently in stock", example = "5")
            int available,

            @Schema(description = "Human-readable explanation",
                    example = "Insufficient stock for 'Widget': requested 10, available 5")
            String message,

            @Schema(description = "In-stock products suggested as replacements, at most three")
            List<AlternativeProduct> alternatives) {
    }

    /**
     * An in-stock product suggested as a replacement for a missing position.
     *
     * @param productId the product ID in MoySklad
     * @param name      the product name
     * @param article   the product article, or an empty string when absent
     * @param price     the first sale price, or {@code null} when no price is available
     * @param stock     the current stock quantity
     */
    @Schema(description = "An in-stock product suggested as a replacement")
    public record AlternativeProduct(
            @Schema(description = "Product ID in MoySklad", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            String productId,

            @Schema(description = "Product name", example = "Widget")
            String name,

            @Schema(description = "Product article", example = "ART-1")
            String article,

            @Schema(description = "First sale price, or null when no price is available", example = "100.00")
            BigDecimal price,

            @Schema(description = "Current stock quantity", example = "5")
            BigDecimal stock) {
    }
}
