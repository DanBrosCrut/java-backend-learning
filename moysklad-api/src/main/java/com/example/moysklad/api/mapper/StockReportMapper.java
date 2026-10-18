package com.example.moysklad.api.mapper;

import com.example.moysklad.api.client.dto.RemapStockReportResponse;
import com.example.moysklad.api.client.dto.RemapStockRow;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses the raw MoySklad stock report JSON into a lookup map of product ID to stock quantity.
 */
@Component
public class StockReportMapper {

    private final ObjectMapper objectMapper;

    /**
     * Creates the mapper with the Spring-configured JSON object mapper.
     *
     * @param objectMapper the Jackson object mapper
     */
    public StockReportMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Deserializes the stock report JSON and maps product IDs to their stock quantities.
     * The product ID is extracted as the last path segment of {@code assortment.meta.href}.
     *
     * @param stockReportJson the raw JSON string from {@code /report/stock/all}
     * @return a map of product ID to stock quantity; rows without a resolvable ID are skipped
     */
    public Map<String, BigDecimal> toStockMap(String stockReportJson) {
        RemapStockReportResponse response = objectMapper.readValue(stockReportJson, RemapStockReportResponse.class);
        if (response.rows() == null) {
            return Map.of();
        }
        return response.rows().stream()
                .filter(row -> extractProductId(row) != null)
                .collect(Collectors.toMap(
                        StockReportMapper::extractProductId,
                        row -> row.quantity() == null ? BigDecimal.ZERO : row.quantity(),
                        (existing, replacement) -> existing));
    }

    private static String extractProductId(RemapStockRow row) {
        if (row.assortment() == null
                || row.assortment().meta() == null
                || row.assortment().meta().href() == null) {
            return null;
        }
        String href = row.assortment().meta().href();
        int lastSlash = href.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == href.length() - 1) {
            return null;
        }
        return href.substring(lastSlash + 1);
    }
}
