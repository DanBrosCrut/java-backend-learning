package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Paginated response wrapper returned by catalog endpoints.
 *
 * @param items the items of the requested page
 * @param total the total number of items matching the request before pagination
 * @param page  the zero-based page index that was returned
 * @param size  the requested page size
 * @param <T>   the item type
 */
@Schema(description = "Paginated response: one page of items plus the total number of matching items")
public record PagedResponse<T>(
        @Schema(description = "Items of the requested page")
        List<T> items,
        @Schema(description = "Total number of items matching the request before pagination", example = "100")
        long total,
        @Schema(description = "Zero-based index of the returned page", example = "0")
        int page,
        @Schema(description = "Requested page size", example = "20")
        int size) {

    /**
     * Wraps the full result list into a single page.
     * Pages beyond the end of the list are returned as empty items with the real total.
     *
     * @param all  the full list of items matching the request
     * @param page the zero-based page index
     * @param size the page size
     * @param <T>  the item type
     * @return the requested page
     */
    public static <T> PagedResponse<T> of(List<T> all, int page, int size) {
        List<T> source = all == null ? List.of() : all;
        long offset = (long) page * size;
        int from = (int) Math.min(offset, source.size());
        int to = (int) Math.min(offset + size, source.size());
        return new PagedResponse<>(List.copyOf(source.subList(from, to)), source.size(), page, size);
    }
}
