package com.example.moysklad.api.client.dto;

import java.math.BigDecimal;

/**
 * A sale price entry within an assortment row of the MoySklad Remap 1.2 API.
 *
 * @param value     the price value in minor currency units
 * @param priceType the price type the value belongs to, e.g. {@code Цена продажи}
 */
public record RemapSalePrice(BigDecimal value, RemapPriceType priceType) {
}
