package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapStockCurrentRow;
import com.example.moysklad.api.config.StockCacheProperties;
import com.example.moysklad.api.exception.MoyskladApiException;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link StockCache}: the 5-minute TTL, the 24-hour full-sync boundary, delta merging
 * and forced refreshes.
 */
@ExtendWith(MockitoExtension.class)
class StockCacheTest {

    private static final String FIRST_PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SECOND_PRODUCT_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String THIRD_PRODUCT_ID = "11111111-2222-3333-4444-555555555555";

    private static final Instant START = Instant.parse("2026-11-09T10:00:00Z");

    @Mock
    private MoyskladClient moyskladClient;

    private MutableClock clock;
    private StockCache cache;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(START, ZoneOffset.UTC);
        cache = new StockCache(moyskladClient, new StockReportMapper(), StockCacheProperties.defaults(), clock);
    }

    @Test
    void get_firstCall_loadsFullReportAndReportsMiss() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(
                row(FIRST_PRODUCT_ID, "5"),
                row(SECOND_PRODUCT_ID, "2")));

        CachedResult<Map<String, BigDecimal>> result = cache.get(false);

        assertThat(result.status()).isEqualTo(CacheStatus.MISS);
        assertThat(result.body())
                .containsEntry(FIRST_PRODUCT_ID, new BigDecimal("5"))
                .containsEntry(SECOND_PRODUCT_ID, new BigDecimal("2"));
        verify(moyskladClient, times(1)).getCurrentStocks();
        verify(moyskladClient, never()).getCurrentStocks(any());
    }

    @Test
    void get_withinFiveMinutes_servesCacheWithoutAnyApiCall() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(
                new RemapStockCurrentRow(FIRST_PRODUCT_ID, new BigDecimal("5"))));

        cache.get(false);
        clock.advance(Duration.ofMinutes(4).plusSeconds(59));
        CachedResult<Map<String, BigDecimal>> second = cache.get(false);

        assertThat(second.status()).isEqualTo(CacheStatus.HIT);
        assertThat(second.body()).containsEntry(FIRST_PRODUCT_ID, new BigDecimal("5"));
        verify(moyskladClient, times(1)).getCurrentStocks();
        verify(moyskladClient, never()).getCurrentStocks(any());
    }

    @Test
    void get_customTtl_isApplied() {
        StockCache customCache = new StockCache(
                moyskladClient,
                new StockReportMapper(),
                new StockCacheProperties(Duration.ofSeconds(30), Duration.ofHours(24)),
                clock);
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(row(FIRST_PRODUCT_ID, "5")));
        when(moyskladClient.getCurrentStocks(START)).thenReturn(List.of(row(FIRST_PRODUCT_ID, "6")));

        customCache.get(false);
        clock.advance(Duration.ofSeconds(29));
        CachedResult<Map<String, BigDecimal>> cached = customCache.get(false);
        clock.advance(Duration.ofSeconds(1));
        CachedResult<Map<String, BigDecimal>> refreshed = customCache.get(false);

        assertThat(cached.status()).isEqualTo(CacheStatus.HIT);
        assertThat(refreshed.status()).isEqualTo(CacheStatus.MISS);
        assertThat(refreshed.body()).containsEntry(FIRST_PRODUCT_ID, new BigDecimal("6"));
        verify(moyskladClient, times(1)).getCurrentStocks();
        verify(moyskladClient, times(1)).getCurrentStocks(START);
    }

    @Test
    void get_afterTtlWithin24Hours_fetchesDeltaAndMergesChanges() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(
                row(FIRST_PRODUCT_ID, "5"),
                row(SECOND_PRODUCT_ID, "2")));
        when(moyskladClient.getCurrentStocks(START)).thenReturn(List.of(
                row(FIRST_PRODUCT_ID, "9"),
                row(SECOND_PRODUCT_ID, "0"),
                row(THIRD_PRODUCT_ID, "4")));

        cache.get(false);
        clock.advance(Duration.ofMinutes(6));
        CachedResult<Map<String, BigDecimal>> delta = cache.get(false);

        assertThat(delta.status()).isEqualTo(CacheStatus.MISS);
        assertThat(delta.body())
                .containsEntry(FIRST_PRODUCT_ID, new BigDecimal("9"))
                .containsEntry(SECOND_PRODUCT_ID, BigDecimal.ZERO)
                .containsEntry(THIRD_PRODUCT_ID, new BigDecimal("4"));
        verify(moyskladClient, times(1)).getCurrentStocks();
        verify(moyskladClient, times(1)).getCurrentStocks(START);
    }

    @Test
    void get_atExactlyTwentyFourHours_fetchesFullReportAndReplacesCache() {
        when(moyskladClient.getCurrentStocks())
                .thenReturn(List.of(row(FIRST_PRODUCT_ID, "5"), row(SECOND_PRODUCT_ID, "2")))
                .thenReturn(List.of(row(FIRST_PRODUCT_ID, "1")));

        cache.get(false);
        clock.advance(Duration.ofHours(24));
        CachedResult<Map<String, BigDecimal>> full = cache.get(false);

        assertThat(full.status()).isEqualTo(CacheStatus.MISS);
        assertThat(full.body()).containsOnly(Map.entry(FIRST_PRODUCT_ID, new BigDecimal("1")));
        verify(moyskladClient, times(2)).getCurrentStocks();
        verify(moyskladClient, never()).getCurrentStocks(any());
    }

    @Test
    void get_justBeforeTwentyFourHours_fetchesDelta() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(row(FIRST_PRODUCT_ID, "5")));
        when(moyskladClient.getCurrentStocks(START)).thenReturn(List.of(row(FIRST_PRODUCT_ID, "7")));

        cache.get(false);
        clock.advance(Duration.ofHours(23).plusMinutes(59));
        CachedResult<Map<String, BigDecimal>> delta = cache.get(false);

        assertThat(delta.status()).isEqualTo(CacheStatus.MISS);
        assertThat(delta.body()).containsEntry(FIRST_PRODUCT_ID, new BigDecimal("7"));
        verify(moyskladClient, times(1)).getCurrentStocks();
        verify(moyskladClient, times(1)).getCurrentStocks(START);
    }

    @Test
    void get_forceRefreshWithinTtl_loadsFullReportAgain() {
        when(moyskladClient.getCurrentStocks())
                .thenReturn(List.of(row(FIRST_PRODUCT_ID, "5")))
                .thenReturn(List.of(row(FIRST_PRODUCT_ID, "9")));

        cache.get(false);
        clock.advance(Duration.ofMinutes(1));
        CachedResult<Map<String, BigDecimal>> forced = cache.get(true);

        assertThat(forced.status()).isEqualTo(CacheStatus.MISS);
        assertThat(forced.body()).containsEntry(FIRST_PRODUCT_ID, new BigDecimal("9"));
        verify(moyskladClient, times(2)).getCurrentStocks();
        verify(moyskladClient, never()).getCurrentStocks(any());
    }

    @Test
    void invalidate_dropsSnapshotSoNextCallLoadsFullReport() {
        when(moyskladClient.getCurrentStocks())
                .thenReturn(List.of(row(FIRST_PRODUCT_ID, "5")))
                .thenReturn(List.of(row(FIRST_PRODUCT_ID, "9")));

        cache.get(false);
        clock.advance(Duration.ofMinutes(1));
        cache.invalidate();
        CachedResult<Map<String, BigDecimal>> afterInvalidation = cache.get(false);

        assertThat(afterInvalidation.status()).isEqualTo(CacheStatus.MISS);
        assertThat(afterInvalidation.body()).containsEntry(FIRST_PRODUCT_ID, new BigDecimal("9"));
        verify(moyskladClient, times(2)).getCurrentStocks();
        verify(moyskladClient, never()).getCurrentStocks(any());
    }

    @Test
    void get_failedInitialLoad_isNotCachedAndNextCallRetries() {
        when(moyskladClient.getCurrentStocks())
                .thenThrow(new MoyskladApiException("upstream failure"))
                .thenReturn(List.of(row(FIRST_PRODUCT_ID, "5")));

        assertThatThrownBy(() -> cache.get(false))
                .isInstanceOf(MoyskladApiException.class);

        CachedResult<Map<String, BigDecimal>> afterFailure = cache.get(false);

        assertThat(afterFailure.status()).isEqualTo(CacheStatus.MISS);
        assertThat(afterFailure.body()).containsEntry(FIRST_PRODUCT_ID, new BigDecimal("5"));
        verify(moyskladClient, times(2)).getCurrentStocks();
    }

    @Test
    void get_failedDelta_keepsPreviousSyncTimeAndNextCallRetries() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(row(FIRST_PRODUCT_ID, "5")));
        when(moyskladClient.getCurrentStocks(START))
                .thenThrow(new MoyskladApiException("delta failure"))
                .thenReturn(List.of(row(FIRST_PRODUCT_ID, "7")));

        cache.get(false);
        clock.advance(Duration.ofMinutes(6));

        assertThatThrownBy(() -> cache.get(false))
                .isInstanceOf(MoyskladApiException.class);

        CachedResult<Map<String, BigDecimal>> retried = cache.get(false);

        assertThat(retried.status()).isEqualTo(CacheStatus.MISS);
        assertThat(retried.body()).containsEntry(FIRST_PRODUCT_ID, new BigDecimal("7"));
        verify(moyskladClient, times(1)).getCurrentStocks();
        verify(moyskladClient, times(2)).getCurrentStocks(START);
    }

    private static RemapStockCurrentRow row(String productId, String stock) {
        return new RemapStockCurrentRow(productId, new BigDecimal(stock));
    }

    /**
     * Mutable clock that lets tests move time forward without sleeping.
     */
    private static final class MutableClock extends Clock {

        private final ZoneId zone;
        private Instant now;

        private MutableClock(Instant now, ZoneId zone) {
            this.now = now;
            this.zone = zone;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
