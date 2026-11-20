package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapStockCurrentRow;
import com.example.moysklad.api.config.StockCacheProperties;
import com.example.moysklad.api.dto.AvailabilityCheckRequest;
import com.example.moysklad.api.dto.AvailabilityCheckRequest.Position;
import com.example.moysklad.api.dto.AvailabilityCheckResponse;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Happy-path tests for {@link AvailabilityService}.
 */
@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final String PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SECOND_PRODUCT_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    private static final List<RemapStockCurrentRow> STOCK_ROWS = List.of(
            new RemapStockCurrentRow(PRODUCT_ID, new BigDecimal("5")),
            new RemapStockCurrentRow(SECOND_PRODUCT_ID, new BigDecimal("2")));

    @Mock
    private MoyskladClient moyskladClient;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(
                new StockCache(moyskladClient, new StockReportMapper(),
                        StockCacheProperties.defaults(), Clock.systemUTC()));
    }

    @Test
    void checkAvailability_enoughStock_returnsAvailableTrueAndEmptyMissing() {
        when(moyskladClient.getCurrentStocks()).thenReturn(STOCK_ROWS);
        AvailabilityCheckRequest request = new AvailabilityCheckRequest(List.of(
                new Position(PRODUCT_ID, 5),
                new Position(SECOND_PRODUCT_ID, 2)));

        AvailabilityCheckResponse response = availabilityService.checkAvailability(request);

        assertThat(response.available()).isTrue();
        assertThat(response.missing()).isEmpty();
    }

    @Test
    void checkAvailability_insufficientStock_returnsAvailableFalseWithMissingPositions() {
        when(moyskladClient.getCurrentStocks()).thenReturn(STOCK_ROWS);
        AvailabilityCheckRequest request = new AvailabilityCheckRequest(List.of(
                new Position(PRODUCT_ID, 10),
                new Position(SECOND_PRODUCT_ID, 1)));

        AvailabilityCheckResponse response = availabilityService.checkAvailability(request);

        assertThat(response.available()).isFalse();
        assertThat(response.missing()).containsExactly(
                new AvailabilityCheckResponse.MissingPosition(PRODUCT_ID, 10, 5));
    }

    @Test
    void checkAvailability_productAbsentFromStockReport_countsAsZeroAvailable() {
        when(moyskladClient.getCurrentStocks()).thenReturn(STOCK_ROWS);
        AvailabilityCheckRequest request = new AvailabilityCheckRequest(List.of(
                new Position("00000000-0000-0000-0000-000000000000", 1)));

        AvailabilityCheckResponse response = availabilityService.checkAvailability(request);

        assertThat(response.available()).isFalse();
        assertThat(response.missing()).containsExactly(
                new AvailabilityCheckResponse.MissingPosition("00000000-0000-0000-0000-000000000000", 1, 0));
    }
}
