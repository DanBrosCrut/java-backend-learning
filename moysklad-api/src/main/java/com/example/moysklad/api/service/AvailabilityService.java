package com.example.moysklad.api.service;

import com.example.moysklad.api.dto.AvailabilityCheckRequest;
import com.example.moysklad.api.dto.AvailabilityCheckResponse;
import com.example.moysklad.api.dto.AvailabilityCheckResponse.MissingPosition;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service that checks whether requested product quantities are available in stock.
 */
@Service
public class AvailabilityService {

    private final StockCache stockCache;

    /**
     * Creates the service with the current stock cache.
     *
     * @param stockCache the in-memory cache of the current stock report
     */
    public AvailabilityService(StockCache stockCache) {
        this.stockCache = stockCache;
    }

    /**
     * Checks the requested positions against the current stock report.
     * A position is missing when the requested quantity exceeds the stock on hand;
     * products absent from the stock report count as zero available.
     *
     * @param request the availability check request
     * @return the availability result with the list of missing positions
     */
    public AvailabilityCheckResponse checkAvailability(AvailabilityCheckRequest request) {
        Map<String, BigDecimal> stocks = stockCache.get(false).body();
        List<MissingPosition> missing = java.util.Optional.ofNullable(request.positions())
                .map(positions -> positions.stream()
                        .filter(position -> {
                            BigDecimal available = stocks.getOrDefault(position.id(), BigDecimal.ZERO);
                            BigDecimal requested = BigDecimal.valueOf(position.quantity());
                            return requested.compareTo(available) > 0;
                        })
                        .map(position -> {
                            BigDecimal available = stocks.getOrDefault(position.id(), BigDecimal.ZERO);
                            return new MissingPosition(position.id(), position.quantity(), available.intValue());
                        })
                        .toList())
                .orElse(List.of());
        return new AvailabilityCheckResponse(missing.isEmpty(), missing);
    }
}
