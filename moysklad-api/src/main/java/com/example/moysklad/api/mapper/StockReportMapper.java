package com.example.moysklad.api.mapper;

import com.example.moysklad.api.client.dto.RemapStockCurrentRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maps the current stock report rows to a lookup map of product ID to stock quantity.
 */
@Component
public class StockReportMapper {

    /**
     * Maps current stock rows to a product ID to stock quantity lookup.
     * Rows without a resolvable product ID are skipped; a missing quantity counts as zero.
     *
     * @param rows the rows of {@code /report/stock/all/current}; {@code null} means no stock
     * @return an immutable map of product ID to current stock quantity
     */
    public Map<String, BigDecimal> toStockMap(List<RemapStockCurrentRow> rows) {
        if (rows == null) {
            return Map.of();
        }
        return rows.stream()
                .filter(row -> row.assortmentId() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                RemapStockCurrentRow::assortmentId,
                                row -> Optional.ofNullable(row.stock()).orElse(BigDecimal.ZERO),
                                (existing, replacement) -> existing),
                        Map::copyOf));
    }
}
