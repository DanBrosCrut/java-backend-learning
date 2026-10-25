package com.example.moysklad.api.dto;

import java.util.List;

/**
 * Request DTO for {@code POST /api/availability} — the list of positions to check.
 *
 * @param positions the requested positions with product ID and quantity
 */
public record AvailabilityRequest(List<Position> positions) {

    /**
     * A single requested position.
     *
     * @param id       the product ID in MoySklad
     * @param quantity the requested quantity
     */
    public record Position(String id, int quantity) {
    }
}
