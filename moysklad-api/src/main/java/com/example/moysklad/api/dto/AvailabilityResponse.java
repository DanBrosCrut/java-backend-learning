package com.example.moysklad.api.dto;

import java.util.List;

/**
 * Response DTO for {@code POST /api/availability} — the result of the availability check.
 *
 * @param available whether every requested position is available in the requested quantity
 * @param missing   the positions that are not available; empty when {@code available} is true
 */
public record AvailabilityResponse(boolean available, List<MissingPosition> missing) {

    /**
     * A requested position that could not be fully covered by the current stock.
     *
     * @param id        the product ID in MoySklad
     * @param requested the requested quantity
     * @param available the quantity currently in stock
     */
    public record MissingPosition(String id, int requested, int available) {
    }
}
