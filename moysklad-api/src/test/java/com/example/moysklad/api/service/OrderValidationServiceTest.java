package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapPriceType;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.client.dto.RemapStockCurrentRow;
import com.example.moysklad.api.dto.OrderValidationRequest;
import com.example.moysklad.api.dto.OrderValidationResponse;
import com.example.moysklad.api.dto.OrderValidationResponse.AlternativeProduct;
import com.example.moysklad.api.dto.OrderValidationResponse.ValidationError;
import com.example.moysklad.api.mapper.ProductMapper;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OrderValidationService}: valid orders, structured errors and alternatives.
 */
@ExtendWith(MockitoExtension.class)
class OrderValidationServiceTest {

    private static final String REQUESTED_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SAME_GROUP_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String ZERO_STOCK_ID = "11111111-2222-3333-4444-555555555555";
    private static final String OTHER_GROUP_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String SECOND_OTHER_GROUP_ID = "99999999-8888-7777-6666-555555555555";
    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    @Mock
    private MoyskladClient moyskladClient;

    private OrderValidationService service;

    @BeforeEach
    void setUp() {
        StockCache stockCache = new StockCache(moyskladClient, new StockReportMapper());
        AssortmentCache assortmentCache = new AssortmentCache(moyskladClient);
        service = new OrderValidationService(
                new AvailabilityService(stockCache),
                assortmentCache,
                stockCache,
                new ProductMapper());
    }

    @Test
    void shouldReturnValid_whenAllPositionsAreInStock() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(stock(REQUESTED_ID, "5")));

        OrderValidationResponse response = service.validate(request(position(REQUESTED_ID, 2)));

        assertThat(response.valid()).isTrue();
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void shouldReturnStructuredErrorWithAlternatives_whenStockIsInsufficient() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(
                stock(REQUESTED_ID, "1"),
                stock(SAME_GROUP_ID, "3"),
                stock(ZERO_STOCK_ID, "0"),
                stock(OTHER_GROUP_ID, "7"),
                stock(SECOND_OTHER_GROUP_ID, "2")));
        when(moyskladClient.getAssortment()).thenReturn(assortment(
                product(REQUESTED_ID, "Requested", "Tools", "500"),
                product(SAME_GROUP_ID, "Alpha", "Tools", "10000"),
                product(ZERO_STOCK_ID, "Zero", "Tools", "20000"),
                product(OTHER_GROUP_ID, "Beta", "Other", "30000"),
                product(SECOND_OTHER_GROUP_ID, "Gamma", "Other", "40000")));

        OrderValidationResponse response = service.validate(request(position(REQUESTED_ID, 5)));

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).singleElement().satisfies(error -> {
            assertThat(error.productId()).isEqualTo(REQUESTED_ID);
            assertThat(error.requested()).isEqualTo(5);
            assertThat(error.available()).isEqualTo(1);
            assertThat(error.message())
                    .isEqualTo("Insufficient stock for 'Requested': requested 5, available 1");
            assertThat(error.alternatives())
                    .extracting(AlternativeProduct::productId)
                    .containsExactly(SAME_GROUP_ID, OTHER_GROUP_ID, SECOND_OTHER_GROUP_ID);
        });
    }

    @Test
    void shouldReturnAlternativeDetails_whenAlternativeIsInStock() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(
                stock(REQUESTED_ID, "0"),
                stock(SAME_GROUP_ID, "3")));
        when(moyskladClient.getAssortment()).thenReturn(assortment(
                product(REQUESTED_ID, "Requested", "Tools", "500"),
                product(SAME_GROUP_ID, "Alpha", "Tools", "10000")));

        OrderValidationResponse response = service.validate(request(position(REQUESTED_ID, 1)));

        assertThat(response.errors()).singleElement()
                .satisfies(error -> assertThat(error.alternatives()).singleElement().satisfies(alternative -> {
                    assertThat(alternative.productId()).isEqualTo(SAME_GROUP_ID);
                    assertThat(alternative.name()).isEqualTo("Alpha");
                    assertThat(alternative.article()).isEqualTo("ART-" + SAME_GROUP_ID);
                    assertThat(alternative.price()).isEqualByComparingTo("100.00");
                    assertThat(alternative.stock()).isEqualByComparingTo("3");
                }));
    }

    @Test
    void shouldReturnNoAlternatives_whenNoOtherProductIsInStock() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(
                stock(REQUESTED_ID, "0"),
                stock(ZERO_STOCK_ID, "0")));
        when(moyskladClient.getAssortment()).thenReturn(assortment(
                product(REQUESTED_ID, "Requested", "Tools", "500"),
                product(ZERO_STOCK_ID, "Zero", "Tools", "20000")));

        OrderValidationResponse response = service.validate(request(position(REQUESTED_ID, 1)));

        assertThat(response.errors()).singleElement()
                .satisfies(error -> assertThat(error.alternatives()).isEmpty());
    }

    @Test
    void shouldAggregateDuplicatePositions_whenSameProductAppearsTwice() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(stock(REQUESTED_ID, "5")));
        when(moyskladClient.getAssortment()).thenReturn(assortment(
                product(REQUESTED_ID, "Requested", "Tools", "500")));

        OrderValidationResponse response = service.validate(request(
                position(REQUESTED_ID, 3),
                position(REQUESTED_ID, 3)));

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).singleElement().satisfies(error -> {
            assertThat(error.productId()).isEqualTo(REQUESTED_ID);
            assertThat(error.requested()).isEqualTo(6);
            assertThat(error.available()).isEqualTo(5);
        });
    }

    @Test
    void shouldReportEveryMissingPosition_whenSeveralPositionsAreShort() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(
                stock(REQUESTED_ID, "1"),
                stock(SAME_GROUP_ID, "0")));
        when(moyskladClient.getAssortment()).thenReturn(assortment(
                product(REQUESTED_ID, "Requested", "Tools", "500"),
                product(SAME_GROUP_ID, "Alpha", "Tools", "10000")));

        OrderValidationResponse response = service.validate(request(
                position(REQUESTED_ID, 2),
                position(SAME_GROUP_ID, 1)));

        assertThat(response.valid()).isFalse();
        assertThat(response.errors())
                .extracting(ValidationError::productId)
                .containsExactly(REQUESTED_ID, SAME_GROUP_ID);
    }

    @Test
    void shouldUseProductIdInMessage_whenProductIsAbsentFromAssortment() {
        when(moyskladClient.getCurrentStocks()).thenReturn(List.of(stock(SAME_GROUP_ID, "3")));
        when(moyskladClient.getAssortment()).thenReturn(assortment(
                product(SAME_GROUP_ID, "Alpha", "Tools", "10000")));

        OrderValidationResponse response = service.validate(request(position(UNKNOWN_ID, 1)));

        assertThat(response.errors()).singleElement().satisfies(error -> {
            assertThat(error.available()).isZero();
            assertThat(error.message())
                    .isEqualTo("Insufficient stock for product " + UNKNOWN_ID + ": requested 1, available 0");
            assertThat(error.alternatives())
                    .extracting(AlternativeProduct::productId)
                    .containsExactly(SAME_GROUP_ID);
        });
    }

    private static OrderValidationRequest request(OrderValidationRequest.Position... positions) {
        return new OrderValidationRequest(List.of(positions));
    }

    private static OrderValidationRequest.Position position(String productId, int quantity) {
        return new OrderValidationRequest.Position(productId, quantity);
    }

    private static RemapStockCurrentRow stock(String productId, String quantity) {
        return new RemapStockCurrentRow(productId, new BigDecimal(quantity));
    }

    private static RemapAssortmentRow product(String id, String name, String group, String price) {
        return new RemapAssortmentRow(
                id,
                name,
                "ART-" + id,
                group,
                List.of(new RemapSalePrice(
                        new BigDecimal(price), new RemapPriceType("Цена продажи"))));
    }

    private static RemapAssortmentResponse assortment(RemapAssortmentRow... rows) {
        return new RemapAssortmentResponse(List.of(rows));
    }
}
