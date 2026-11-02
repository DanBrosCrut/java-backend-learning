package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Validated sorting parameters of the catalog request.
 * A {@code null} field means no sorting is applied and the MoySklad assortment order is kept.
 *
 * @param field the product field to sort by, or {@code null} for the natural order
 * @param order the sort direction
 */
@Schema(description = "Sorting parameters of the catalog request")
public record SortOptions(
        @Schema(description = "Field to sort by: name, article, group, price or stock; empty keeps the MoySklad order", example = "price")
        CatalogField field,
        @Schema(description = "Sort direction: asc or desc", example = "asc")
        SortOrder order) {

    /**
     * Returns the sorting that keeps the natural MoySklad assortment order.
     *
     * @return the natural sorting
     */
    public static SortOptions natural() {
        return new SortOptions(null, SortOrder.ASC);
    }

    /**
     * Checks whether no sorting is applied.
     *
     * @return {@code true} when no sort field is set
     */
    public boolean isEmpty() {
        return field == null;
    }
}
