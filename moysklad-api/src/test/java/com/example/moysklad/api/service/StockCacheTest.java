package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapStockCurrentRow;
import com.example.moysklad.api.exception.MoyskladApiException;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link StockCache}: HIT/MISS reporting and invalidation of the current stock report.
 */
@ExtendWith(MockitoExtension.class)
class StockCacheTest {

    private static final String FIRST_PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SECOND_PRODUCT_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    @Mock
    private MoyskladClient moyskladClient;

    private StockCache cache;

    @BeforeEach
    void setUp() {
        cache = new StockCache(moyskladClient, new StockReportMapper());
    }

    @Test
    void get_firstCall_reportsMissAndMapsStocks() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(
                new RemapStockCurrentRow(FIRST_PRODUCT_ID, new BigDecimal("5")),
                new RemapStockCurrentRow(SECOND_PRODUCT_ID, new BigDecimal("2"))));

        CachedResult<Map<String, BigDecimal>> result = cache.get(false);

        assertThat(result.status()).isEqualTo(CacheStatus.MISS);
        assertThat(result.body())
                .containsEntry(FIRST_PRODUCT_ID, new BigDecimal("5"))
                .containsEntry(SECOND_PRODUCT_ID, new BigDecimal("2"));
        verify(moyskladClient, times(1)).getCurrentStocks();
    }

    @Test
    void get_secondCall_reportsHitWithoutCallingMoySklad() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(
                new RemapStockCurrentRow(FIRST_PRODUCT_ID, new BigDecimal("5"))));

        cache.get(false);
        CachedResult<Map<String, BigDecimal>> second = cache.get(false);

        assertThat(second.status()).isEqualTo(CacheStatus.HIT);
        assertThat(second.body()).containsEntry(FIRST_PRODUCT_ID, new BigDecimal("5"));
        verify(moyskladClient, times(1)).getCurrentStocks();
    }

    @Test
    void get_invalidateTrue_reloadsStocksAndReportsMiss() {
        when(moyskladClient.getCurrentStocks())
                .thenReturn(List.of(new RemapStockCurrentRow(FIRST_PRODUCT_ID, new BigDecimal("5"))))
                .thenReturn(List.of(new RemapStockCurrentRow(FIRST_PRODUCT_ID, new BigDecimal("9"))));

        cache.get(false);
        CachedResult<Map<String, BigDecimal>> reloaded = cache.get(true);

        assertThat(reloaded.status()).isEqualTo(CacheStatus.MISS);
        assertThat(reloaded.body()).containsEntry(FIRST_PRODUCT_ID, new BigDecimal("9"));
        verify(moyskladClient, times(2)).getCurrentStocks();
    }

    @Test
    void invalidate_alone_dropsSnapshotSoNextCallIsMiss() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(
                new RemapStockCurrentRow(FIRST_PRODUCT_ID, new BigDecimal("5"))));

        cache.get(false);
        cache.invalidate();
        CachedResult<Map<String, BigDecimal>> afterInvalidation = cache.get(false);

        assertThat(afterInvalidation.status()).isEqualTo(CacheStatus.MISS);
        verify(moyskladClient, times(2)).getCurrentStocks();
    }

    @Test
    void get_failedLoad_isNotCachedAndNextCallRetries() {
        when(moyskladClient.getCurrentStocks())
                .thenThrow(new MoyskladApiException("upstream failure"))
                .thenReturn(List.of(new RemapStockCurrentRow(FIRST_PRODUCT_ID, new BigDecimal("5"))));

        assertThatThrownBy(() -> cache.get(false))
                .isInstanceOf(MoyskladApiException.class);

        CachedResult<Map<String, BigDecimal>> afterFailure = cache.get(false);

        assertThat(afterFailure.status()).isEqualTo(CacheStatus.MISS);
        assertThat(afterFailure.body()).containsEntry(FIRST_PRODUCT_ID, new BigDecimal("5"));
        verify(moyskladClient, times(2)).getCurrentStocks();
    }
}
