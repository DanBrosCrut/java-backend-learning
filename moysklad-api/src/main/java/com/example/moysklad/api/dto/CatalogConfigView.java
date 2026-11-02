package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Available catalog display options for the current MoySklad account.
 *
 * @param priceTypes the price type names found in the assortment
 * @param groups     the product group names found in the assortment
 * @param fields     the field names supported by the {@code fields} option
 * @param defaults   the options applied when the client passes no query parameters
 */
@Schema(description = "Available showcase options and their defaults for the current MoySklad account")
public record CatalogConfigView(
        @Schema(description = "Available price type names")
        List<String> priceTypes,
        @Schema(description = "Available product group names")
        List<String> groups,
        @Schema(description = "Supported field names for the 'fields' option")
        List<String> fields,
        @Schema(description = "Default options applied when no query parameters are passed")
        CatalogOptions defaults) {
}
