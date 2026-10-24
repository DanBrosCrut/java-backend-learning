package com.example.moysklad.api.mapper;

import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.dto.ProductView;
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
     * @param row   the Remap assortment row
     * @param stock the current stock quantity of the product, or {@code null} if unknown
     * @return the mapped product view with stock defaulting to zero when unknown
     */
    public ProductView toDto(RemapAssortmentRow row, BigDecimal stock) {
        return new ProductView(
                row.id(),
                row.name(),
                row.article() == null ? "" : row.article(),
                extractPrice(row.salePrices()),
                stock == null ? BigDecimal.ZERO : stock);
    }

    private String extractPrice(List<RemapSalePrice> salePrices) {
        if (salePrices == null || salePrices.isEmpty()) {
            return "";
        }
        String value = salePrices.get(0).value();
        if (value == null || value.isBlank()) {
            return "";
        }
        // Parse as BigDecimal from string to avoid floating-point artifacts, then convert from minor units
        BigDecimal priceInMinorUnits = new BigDecimal(value);
        return priceInMinorUnits.movePointLeft(2).toPlainString();
    }
}
