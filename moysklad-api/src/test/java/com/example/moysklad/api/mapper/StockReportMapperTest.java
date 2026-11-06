package com.example.moysklad.api.mapper;

import com.example.moysklad.api.client.dto.RemapStockCurrentRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link StockReportMapper}: mapping current stock rows to a lookup map.
 */
class StockReportMapperTest {

    private static final String PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SECOND_PRODUCT_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    private final StockReportMapper mapper = new StockReportMapper();

    @Test
    void toStockMap_rows_areMappedByProductId() {
        Map<String, BigDecimal> stocks = mapper.toStockMap(List.of(
                new RemapStockCurrentRow(PRODUCT_ID, new BigDecimal("5")),
                new RemapStockCurrentRow(SECOND_PRODUCT_ID, new BigDecimal("2.5"))));

        assertThat(stocks)
                .containsEntry(PRODUCT_ID, new BigDecimal("5"))
                .containsEntry(SECOND_PRODUCT_ID, new BigDecimal("2.5"));
    }

    @Test
    void toStockMap_missingStock_countsAsZero() {
        Map<String, BigDecimal> stocks = mapper.toStockMap(List.of(
                new RemapStockCurrentRow(PRODUCT_ID, null)));

        assertThat(stocks).containsEntry(PRODUCT_ID, BigDecimal.ZERO);
    }

    @Test
    void toStockMap_rowWithoutProductId_isSkipped() {
        Map<String, BigDecimal> stocks = mapper.toStockMap(List.of(
                new RemapStockCurrentRow(null, new BigDecimal("5")),
                new RemapStockCurrentRow(PRODUCT_ID, new BigDecimal("1"))));

        assertThat(stocks).containsOnlyKeys(PRODUCT_ID);
    }

    @Test
    void toStockMap_duplicateProductId_keepsFirstValue() {
        Map<String, BigDecimal> stocks = mapper.toStockMap(List.of(
                new RemapStockCurrentRow(PRODUCT_ID, new BigDecimal("5")),
                new RemapStockCurrentRow(PRODUCT_ID, new BigDecimal("7"))));

        assertThat(stocks).containsEntry(PRODUCT_ID, new BigDecimal("5"));
    }

    @Test
    void toStockMap_nullRows_returnsEmptyMap() {
        assertThat(mapper.toStockMap(null)).isEmpty();
        assertThat(mapper.toStockMap(List.of())).isEmpty();
    }

    @Test
    void toStockMap_resultIsImmutable() {
        Map<String, BigDecimal> stocks = mapper.toStockMap(List.of(
                new RemapStockCurrentRow(PRODUCT_ID, new BigDecimal("5"))));

        assertThatThrownBy(() -> stocks.put(SECOND_PRODUCT_ID, BigDecimal.ONE))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
