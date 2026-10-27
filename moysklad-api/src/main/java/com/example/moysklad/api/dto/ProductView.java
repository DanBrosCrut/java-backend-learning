package com.example.moysklad.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Product card returned by {@code GET /api/products}.
 * Fields that are not requested through the {@code fields} option are omitted from the JSON.
 *
 * @param id      the product identifier in MoySklad
 * @param name    the product name
 * @param article the product article
 * @param group   the product group (folder) name
 * @param price   the sale price in the currency of the selected price type
 * @param stock   the quantity in stock ({@code 0} when the stock report has no row)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Product card of the MoySklad showcase catalog")
public record ProductView(
        @Schema(description = "Product identifier in MoySklad", example = "123e4567-e89b-12d3-a456-426614174000")
        String id,
        @Schema(description = "Product name", example = "Widget")
        String name,
        @Schema(description = "Product article", example = "ART-1")
        String article,
        @Schema(description = "Product group (folder) name", example = "Товары")
        String group,
        @Schema(description = "Sale price for the selected price type", example = "1000.00")
        BigDecimal price,
        @Schema(description = "Quantity in stock", example = "5")
        BigDecimal stock) {
}
