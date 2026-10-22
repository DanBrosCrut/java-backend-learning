package com.example.moysklad.api.client.dto;

import java.math.BigDecimal;

/**
 * A single stock report row returned by the MoySklad Remap 1.2 API.
 *
 * @param assortment the referenced assortment entity (product, variant, etc.)
 * @param quantity   the current stock quantity
 */
public record RemapStockRow(RemapStockAssortment assortment, BigDecimal quantity) {

    /**
     * The assortment reference embedded in a stock report row.
     *
     * @param meta the entity metadata containing the entity link
     */
    public record RemapStockAssortment(Meta meta) {

        /**
         * Metadata of the referenced assortment entity.
         *
         * @param href the API link to the entity; the product ID is the last path segment
         */
        public record Meta(String href) {
        }
    }
}
