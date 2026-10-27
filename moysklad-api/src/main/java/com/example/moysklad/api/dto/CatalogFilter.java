package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;

/**
 * Free-text filter of the catalog, matched against product name and article.
 *
 * @param query the case-insensitive search query, or {@code null} when no text filter is applied
 */
@Schema(description = "Free-text catalog filter matched against product name and article")
public record CatalogFilter(
        @Schema(description = "Case-insensitive text matched against name and article", example = "машин")
        String query) {

    /**
     * Normalizes the query: trims it, drops blank values and lower-cases it for matching.
     */
    public CatalogFilter {
        query = query == null || query.isBlank() ? null : query.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a filter that matches every product.
     *
     * @return the empty filter
     */
    public static CatalogFilter none() {
        return new CatalogFilter(null);
    }

    /**
     * Checks whether a text filter is applied.
     *
     * @return {@code true} when no query is set
     */
    public boolean isEmpty() {
        return query == null;
    }
}
