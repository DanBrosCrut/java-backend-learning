package com.example.moysklad.api.mapper;

import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.dto.CatalogField;
import com.example.moysklad.api.dto.CatalogOptions;
import com.example.moysklad.api.dto.ProductView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Maps MoySklad Remap assortment rows to the public product view, honouring the catalog options.
 */
@Component
public class ProductMapper {

    private static final int MINOR_UNIT_SCALE = 2;

    /**
     * Converts a Remap assortment row to a product view with the requested fields only.
     *
     * @param row     the Remap assortment row
     * @param stock   the current stock quantity of the product, or {@code null} if unknown
     * @param options the catalog display options
     * @return the mapped product view; fields that are not requested are {@code null}
     */
    public ProductView toView(RemapAssortmentRow row, BigDecimal stock, CatalogOptions options) {
        return new ProductView(
                row.id(),
                options.includes(CatalogField.NAME) ? row.name() : null,
                options.includes(CatalogField.ARTICLE) ? Optional.ofNullable(row.article()).orElse("") : null,
                options.includes(CatalogField.GROUP) ? row.pathName() : null,
                options.includes(CatalogField.PRICE)
                        ? extractPrice(row.salePrices(), options.priceType()).orElse(null)
                        : null,
                options.includes(CatalogField.STOCK)
                        ? Optional.ofNullable(stock).orElse(BigDecimal.ZERO)
                        : null);
    }

    /**
     * Extracts the sale price of the requested price type and converts it from minor units to a money value.
     * When no price type is requested, the first priced entry is used.
     *
     * @param salePrices the sale prices of the assortment row
     * @param priceType  the requested price type name, or {@code null} for the first sale price
     * @return the price with scale 2, or empty when no matching price exists
     */
    public Optional<BigDecimal> extractPrice(List<RemapSalePrice> salePrices, String priceType) {
        return Optional.ofNullable(salePrices).orElse(List.of()).stream()
                .filter(price -> price.value() != null)
                .filter(price -> priceType == null || matchesPriceType(price, priceType))
                .map(RemapSalePrice::value)
                .findFirst()
                .map(value -> value.movePointLeft(MINOR_UNIT_SCALE).setScale(MINOR_UNIT_SCALE, RoundingMode.HALF_UP));
    }

    private static boolean matchesPriceType(RemapSalePrice price, String priceType) {
        return price.priceType() != null
                && price.priceType().name() != null
                && priceType.equalsIgnoreCase(price.priceType().name());
    }
}
