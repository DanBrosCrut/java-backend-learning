package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.dto.AvailabilityRequest;
import com.example.moysklad.api.dto.AvailabilityRequest.Position;
import com.example.moysklad.api.dto.AvailabilityResponse;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

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

    private static final String STOCK_REPORT_JSON = """
            {
              "rows": [
                {
                  "assortment": {
                    "meta": {
                      "href": "https://api.moysklad.ru/api/remap/1.2/entity/product/123e4567-e89b-12d3-a456-426614174000"
                    }
                  },
                  "quantity": 5
                },
                {
                  "assortment": {
                    "meta": {
                      "href": "https://api.moysklad.ru/api/remap/1.2/entity/product/7c9e6679-7425-40de-944b-e07fc1f90ae7"
                    }
                  },
                  "quantity": 2
                }
              ]
            }
            """;

    @Mock
    private MoyskladClient moyskladClient;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(
                moyskladClient,
                new StockReportMapper(new ObjectMapper()));
    }

    @Test
    void checkAvailability_enoughStock_returnsAvailableTrueAndEmptyMissing() {
        when(moyskladClient.getStockReport()).thenReturn(STOCK_REPORT_JSON);
        AvailabilityRequest request = new AvailabilityRequest(List.of(
                new Position(PRODUCT_ID, 5),
                new Position(SECOND_PRODUCT_ID, 2)));

        AvailabilityResponse response = availabilityService.checkAvailability(request);

        assertThat(response.available()).isTrue();
        assertThat(response.missing()).isEmpty();
    }

    @Test
    void checkAvailability_insufficientStock_returnsAvailableFalseWithMissingPositions() {
        when(moyskladClient.getStockReport()).thenReturn(STOCK_REPORT_JSON);
        AvailabilityRequest request = new AvailabilityRequest(List.of(
                new Position(PRODUCT_ID, 10),
                new Position(SECOND_PRODUCT_ID, 1)));

        AvailabilityResponse response = availabilityService.checkAvailability(request);

        assertThat(response.available()).isFalse();
        assertThat(response.missing()).containsExactly(
                new AvailabilityResponse.MissingPosition(PRODUCT_ID, 10, 5));
    }

    @Test
    void checkAvailability_productAbsentFromStockReport_countsAsZeroAvailable() {
        when(moyskladClient.getStockReport()).thenReturn(STOCK_REPORT_JSON);
        AvailabilityRequest request = new AvailabilityRequest(List.of(
                new Position("00000000-0000-0000-0000-000000000000", 1)));

        AvailabilityResponse response = availabilityService.checkAvailability(request);

        assertThat(response.available()).isFalse();
        assertThat(response.missing()).containsExactly(
                new AvailabilityResponse.MissingPosition("00000000-0000-0000-0000-000000000000", 1, 0));
    }
}
