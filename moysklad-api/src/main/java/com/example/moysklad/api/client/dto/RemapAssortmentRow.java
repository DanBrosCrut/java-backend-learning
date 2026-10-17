package com.example.moysklad.api.client.dto;

import java.util.List;

/**
 * A single assortment row returned by the MoySklad Remap 1.2 API.
 *
 * @param id         the assortment identifier
 * @param name       the product name
 * @param article    the product article
 * @param salePrices the sale prices
 */
public record RemapAssortmentRow(String id, String name, String article, List<RemapSalePrice> salePrices) {
}
