package com.example.moysklad.api.mapper;

import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.dto.ProductDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Maps MoySklad Remap assortment rows to the public product DTO.
 */
@Component
public class ProductMapper {

    /**
     * Converts a Remap assortment row to a product DTO.
     *
     * @param row the Remap assortment row
     * @return the mapped product DTO
     */
    public ProductDto toDto(RemapAssortmentRow row) {
        return new ProductDto(
                row.id(),
                row.name(),
                row.article() == null ? "" : row.article(),
                extractPrice(row.salePrices()));
    }

    private String extractPrice(List<RemapSalePrice> salePrices) {
        if (salePrices == null || salePrices.isEmpty()) {
            return "";
        }
        String value = salePrices.get(0).value();
        if (value == null || value.isBlank()) {
            return "";
        }
        return BigDecimal.valueOf(new BigDecimal(value).longValue()).movePointLeft(2).toPlainString();
    }
}
