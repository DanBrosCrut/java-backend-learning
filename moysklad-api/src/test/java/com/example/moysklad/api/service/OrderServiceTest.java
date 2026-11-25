package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapCustomerOrderDto;
import com.example.moysklad.api.client.dto.RemapCustomerOrderResponseDto;
import com.example.moysklad.api.client.dto.RemapOrderPositionDto;
import com.example.moysklad.api.client.dto.RemapPriceType;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.config.MoyskladProperties;
import com.example.moysklad.api.config.OrderProperties;
import com.example.moysklad.api.dto.CreateOrderRequest;
import com.example.moysklad.api.dto.OrderResponse;
import com.example.moysklad.api.dto.OrderValidationRequest;
import com.example.moysklad.api.dto.OrderValidationResponse;
import com.example.moysklad.api.exception.OrderConfigurationException;
import com.example.moysklad.api.exception.OrderValidationFailedException;
import com.example.moysklad.api.mapper.CustomerOrderMapper;
import com.example.moysklad.api.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OrderService}: validation gate, Remap payload mapping and creation.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String BASE_URL = "https://api.moysklad.ru/api/remap/1.2";
    private static final String ORGANIZATION_ID = "org-1";
    private static final String AGENT_ID = "agent-1";

    @Mock
    private OrderValidationService orderValidationService;

    @Mock
    private AssortmentCache assortmentCache;

    @Mock
    private MoyskladClient moyskladClient;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        CustomerOrderMapper mapper = new CustomerOrderMapper(
                new MoyskladProperties("test-token", BASE_URL, 1000),
                new ProductMapper());
        orderService = new OrderService(
                orderValidationService,
                mapper,
                assortmentCache,
                moyskladClient,
                new OrderProperties(ORGANIZATION_ID, AGENT_ID));
    }

    @Test
    void shouldCreateOrder_whenAllPositionsAreInStock() {
        when(orderValidationService.validate(any(OrderValidationRequest.class)))
                .thenReturn(new OrderValidationResponse(true, List.of()));
        when(assortmentCache.get(false)).thenReturn(new CachedResult<>(
                assortment(row(PRODUCT_ID, "Widget", "50000")), CacheStatus.HIT));
        when(moyskladClient.createCustomerOrder(any(RemapCustomerOrderDto.class)))
                .thenReturn(new RemapCustomerOrderResponseDto("order-1", "00001", new BigDecimal("1000.00")));

        OrderResponse response = orderService.create(request(PRODUCT_ID, 2));

        assertThat(response.id()).isEqualTo("order-1");
        assertThat(response.name()).isEqualTo("00001");
        assertThat(response.sum()).isEqualByComparingTo("1000.00");

        ArgumentCaptor<RemapCustomerOrderDto> captor = ArgumentCaptor.forClass(RemapCustomerOrderDto.class);
        verify(moyskladClient).createCustomerOrder(captor.capture());
        RemapCustomerOrderDto payload = captor.getValue();
        assertThat(payload.organization().href()).isEqualTo(BASE_URL + "/entity/organization/" + ORGANIZATION_ID);
        assertThat(payload.organization().type()).isEqualTo("organization");
        assertThat(payload.agent().href()).isEqualTo(BASE_URL + "/entity/counterparty/" + AGENT_ID);
        assertThat(payload.agent().type()).isEqualTo("counterparty");
        assertThat(payload.positions()).singleElement().satisfies(position -> {
            assertThat(position.quantity()).isEqualByComparingTo("2");
            assertThat(position.price()).isEqualByComparingTo("500.00");
            assertThat(position.assortment().href()).isEqualTo(BASE_URL + "/entity/product/" + PRODUCT_ID);
            assertThat(position.assortment().type()).isEqualTo("product");
        });
    }

    @Test
    void shouldNotCreateOrder_whenValidationFails() {
        when(orderValidationService.validate(any(OrderValidationRequest.class)))
                .thenReturn(new OrderValidationResponse(false, List.of(
                        new OrderValidationResponse.ValidationError(
                                PRODUCT_ID, 5, 1, "Insufficient stock for 'Widget': requested 5, available 1",
                                List.of()))));

        assertThatThrownBy(() -> orderService.create(request(PRODUCT_ID, 5)))
                .isInstanceOf(OrderValidationFailedException.class)
                .hasMessageContaining("Order validation failed")
                .hasMessageContaining(PRODUCT_ID);

        verify(moyskladClient, never()).createCustomerOrder(any());
    }

    @Test
    void shouldThrowConfigurationError_whenOrganizationOrAgentIsMissing() {
        OrderService unconfigured = new OrderService(
                orderValidationService,
                new CustomerOrderMapper(
                        new MoyskladProperties("test-token", BASE_URL, 1000),
                        new ProductMapper()),
                assortmentCache,
                moyskladClient,
                new OrderProperties("", ""));
        when(orderValidationService.validate(any(OrderValidationRequest.class)))
                .thenReturn(new OrderValidationResponse(true, List.of()));

        assertThatThrownBy(() -> unconfigured.create(request(PRODUCT_ID, 1)))
                .isInstanceOf(OrderConfigurationException.class)
                .hasMessageContaining("organization-id");

        verify(moyskladClient, never()).createCustomerOrder(any());
    }

    @Test
    void shouldUseZeroPrice_whenProductHasNoSalePrice() {
        when(orderValidationService.validate(any(OrderValidationRequest.class)))
                .thenReturn(new OrderValidationResponse(true, List.of()));
        when(assortmentCache.get(false)).thenReturn(new CachedResult<>(
                assortment(new RemapAssortmentRow(PRODUCT_ID, "Widget", "ART-1", "Tools", List.of())),
                CacheStatus.HIT));
        when(moyskladClient.createCustomerOrder(any(RemapCustomerOrderDto.class)))
                .thenReturn(new RemapCustomerOrderResponseDto("order-2", "00002", BigDecimal.ZERO));

        orderService.create(request(PRODUCT_ID, 3));

        ArgumentCaptor<RemapCustomerOrderDto> captor = ArgumentCaptor.forClass(RemapCustomerOrderDto.class);
        verify(moyskladClient).createCustomerOrder(captor.capture());
        assertThat(captor.getValue().positions()).singleElement()
                .extracting(RemapOrderPositionDto::price)
                .satisfies(price -> assertThat((BigDecimal) price).isEqualByComparingTo(BigDecimal.ZERO));
    }

    private static CreateOrderRequest request(String productId, int quantity) {
        return new CreateOrderRequest(List.of(new CreateOrderRequest.Position(productId, quantity)));
    }

    private static RemapAssortmentRow row(String id, String name, String price) {
        return new RemapAssortmentRow(
                id,
                name,
                "ART-" + id,
                "Tools",
                List.of(new RemapSalePrice(
                        new BigDecimal(price), new RemapPriceType("Цена продажи"))));
    }

    private static RemapAssortmentResponse assortment(RemapAssortmentRow... rows) {
        return new RemapAssortmentResponse(List.of(rows));
    }
}
