package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Display options of the catalog, modelled after the MoySklad "Online order" showcase settings.
 *
 * @param priceType  the price type to show, or {@code null} for the first available sale price
 * @param groups     the product groups to show; empty means all groups
 * @param inStockOnly whether products with zero stock are hidden
 * @param fields     the product fields included in the response
 */
@Schema(description = "Catalog display options resolved from the request query parameters")
public record CatalogOptions(
        @Schema(description = "Price type name, e.g. 'Цена продажи'. Empty means the first sale price", example = "Цена продажи")
        String priceType,
        @Schema(description = "Product group names to show. Empty means all groups", example = "[\"Товары\"]")
        List<String> groups,
        @Schema(description = "When true, products with zero stock are hidden", example = "false")
        boolean inStockOnly,
        @Schema(description = "Product fields included in the response", example = "[\"name\",\"price\",\"stock\"]")
        List<CatalogField> fields) {

    /**
     * All fields that are returned when the client does not restrict the field set.
     */
    public static final List<CatalogField> ALL_FIELDS = List.of(CatalogField.values());

    /**
     * Normalizes the options, replacing blank values and empty collections with defaults.
     */
    public CatalogOptions {
        priceType = priceType == null || priceType.isBlank() ? null : priceType.trim();
        groups = groups == null ? List.of() : List.copyOf(groups);
        fields = fields == null || fields.isEmpty() ? ALL_FIELDS : List.copyOf(fields);
    }

    /**
     * Checks whether the given field must be included in the response.
     *
     * @param field the catalog field
     * @return {@code true} when the field is requested
     */
    public boolean includes(CatalogField field) {
        return fields.contains(field);
    }

    /**
     * Returns the options used when the client passes no catalog options.
     *
     * @return default options: first price type, all groups, all fields, stock shown
     */
    public static CatalogOptions defaults() {
        return new CatalogOptions(null, List.of(), false, ALL_FIELDS);
    }
}
