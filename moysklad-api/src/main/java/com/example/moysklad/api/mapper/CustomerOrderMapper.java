package com.example.moysklad.api.mapper;

import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapCustomerOrderDto;
import com.example.moysklad.api.client.dto.RemapCustomerOrderResponseDto;
import com.example.moysklad.api.client.dto.RemapMetaDto;
import com.example.moysklad.api.client.dto.RemapOrderPositionDto;
import com.example.moysklad.api.config.MoyskladProperties;
import com.example.moysklad.api.dto.CreateOrderRequest;
import com.example.moysklad.api.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Maps the public order DTOs to the Remap customer order payload and back.
 */
@Component
public class CustomerOrderMapper {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final MoyskladProperties properties;
    private final ProductMapper productMapper;

    /**
     * Creates the mapper with the MoySklad base URL and the product price mapper.
     *
     * @param properties    the MoySklad configuration
     * @param productMapper the mapper extracting sale prices from assortment rows
     */
    public CustomerOrderMapper(MoyskladProperties properties, ProductMapper productMapper) {
        this.properties = properties;
        this.productMapper = productMapper;
    }

    /**
     * Builds the Remap customer order payload from the client request.
     *
     * @param request          the ordered positions
     * @param rows             the assortment rows used for prices
     * @param pricesByProduct  the sale price per product ID; products without a price are zero
     * @param organizationId   the selling organization ID
     * @param agentId          the buying counterparty ID
     * @return the Remap payload
     */
    public RemapCustomerOrderDto toRemap(CreateOrderRequest request,
                                         List<RemapAssortmentRow> rows,
                                         Map<String, BigDecimal> pricesByProduct,
                                         String organizationId,
                                         String agentId) {
        List<RemapOrderPositionDto> positions = request.positions().stream()
                .map(position -> new RemapOrderPositionDto(
                        BigDecimal.valueOf(position.quantity()),
                        pricesByProduct.getOrDefault(position.productId(), ZERO),
                        meta("product", position.productId())))
                .toList();
        return new RemapCustomerOrderDto(
                meta("organization", organizationId),
                meta("counterparty", agentId),
                positions);
    }

    /**
     * Extracts the sale price of every assortment row into a product ID to price map.
     *
     * @param rows the assortment rows
     * @return the product ID to sale price map; rows without a price map to zero
     */
    public Map<String, BigDecimal> toPriceMap(List<RemapAssortmentRow> rows) {
        return rows.stream()
                .collect(java.util.stream.Collectors.toMap(
                        RemapAssortmentRow::id,
                        row -> productMapper.extractPrice(row.salePrices(), null).orElse(ZERO),
                        (first, ignored) -> first));
    }

    /**
     * Maps the created Remap customer order to the public response.
     *
     * @param created the Remap order response
     * @return the public order response
     */
    public OrderResponse toResponse(RemapCustomerOrderResponseDto created) {
        return new OrderResponse(created.id(), created.name(), created.sum());
    }

    private RemapMetaDto meta(String type, String id) {
        return new RemapMetaDto(
                properties.baseUrl() + "/entity/" + type + "/" + id,
                type,
                "application/json");
    }
}
