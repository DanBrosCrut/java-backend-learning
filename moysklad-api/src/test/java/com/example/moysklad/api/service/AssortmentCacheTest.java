package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.exception.MoyskladApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AssortmentCache}: HIT/MISS reporting and invalidation.
 */
@ExtendWith(MockitoExtension.class)
class AssortmentCacheTest {

    @Mock
    private MoyskladClient moyskladClient;

    private AssortmentCache cache;

    @BeforeEach
    void setUp() {
        cache = new AssortmentCache(moyskladClient);
    }

    @Test
    void get_firstCall_reportsMissAndLoadsSnapshot() {
        when(moyskladClient.getAssortment()).thenReturn(snapshot("1", "2"));

        CachedResult<RemapAssortmentResponse> result = cache.get(false);

        assertThat(result.status()).isEqualTo(CacheStatus.MISS);
        assertThat(result.body().rows())
                .extracting(RemapAssortmentRow::id)
                .containsExactly("1", "2");
        verify(moyskladClient, times(1)).getAssortment();
    }

    @Test
    void get_secondCall_reportsHitWithoutCallingMoySklad() {
        when(moyskladClient.getAssortment()).thenReturn(snapshot("1", "2"));

        cache.get(false);
        CachedResult<RemapAssortmentResponse> second = cache.get(false);

        assertThat(second.status()).isEqualTo(CacheStatus.HIT);
        assertThat(second.body().rows()).hasSize(2);
        verify(moyskladClient, times(1)).getAssortment();
    }

    @Test
    void get_invalidateTrue_reloadsSnapshotAndReportsMiss() {
        when(moyskladClient.getAssortment())
                .thenReturn(snapshot("1", "2"))
                .thenReturn(snapshot("3"));

        cache.get(false);
        CachedResult<RemapAssortmentResponse> reloaded = cache.get(true);

        assertThat(reloaded.status()).isEqualTo(CacheStatus.MISS);
        assertThat(reloaded.body().rows())
                .extracting(RemapAssortmentRow::id)
                .containsExactly("3");
        verify(moyskladClient, times(2)).getAssortment();
    }

    @Test
    void get_afterInvalidation_nextCallReportsHitOnTheReloadedSnapshot() {
        when(moyskladClient.getAssortment())
                .thenReturn(snapshot("1"))
                .thenReturn(snapshot("2"));

        cache.get(false);
        cache.get(true);
        CachedResult<RemapAssortmentResponse> afterReload = cache.get(false);

        assertThat(afterReload.status()).isEqualTo(CacheStatus.HIT);
        assertThat(afterReload.body().rows())
                .extracting(RemapAssortmentRow::id)
                .containsExactly("2");
        verify(moyskladClient, times(2)).getAssortment();
    }

    @Test
    void invalidate_alone_dropsSnapshotSoNextCallIsMiss() {
        when(moyskladClient.getAssortment()).thenReturn(snapshot("1"));

        cache.get(false);
        cache.invalidate();
        CachedResult<RemapAssortmentResponse> afterInvalidation = cache.get(false);

        assertThat(afterInvalidation.status()).isEqualTo(CacheStatus.MISS);
        verify(moyskladClient, times(2)).getAssortment();
    }

    @Test
    void get_failedLoad_isNotCachedAndNextCallRetries() {
        when(moyskladClient.getAssortment())
                .thenThrow(new MoyskladApiException("upstream failure"))
                .thenReturn(snapshot("1"));

        assertThatThrownBy(() -> cache.get(false))
                .isInstanceOf(MoyskladApiException.class);

        CachedResult<RemapAssortmentResponse> afterFailure = cache.get(false);

        assertThat(afterFailure.status()).isEqualTo(CacheStatus.MISS);
        assertThat(afterFailure.body().rows())
                .extracting(RemapAssortmentRow::id)
                .containsExactly("1");
        verify(moyskladClient, times(2)).getAssortment();
    }

    private static RemapAssortmentResponse snapshot(String... ids) {
        return new RemapAssortmentResponse(
                java.util.Arrays.stream(ids)
                        .map(id -> new RemapAssortmentRow(id, "Product " + id, "ART-" + id, "Товары", List.of()))
                        .toList());
    }
}
