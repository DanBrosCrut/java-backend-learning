package com.example.moysklad.api.client.dto;

import java.math.BigDecimal;

/**
 * A single row of the MoySklad Remap 1.2 current stock report ({@code /report/stock/all/current}).
 * <p>
 * The report returns a bare JSON array and, by default ({@code stockType=stock}), only the
 * {@code assortmentId} and the physical {@code stock} fields.
 *
 * @param assortmentId the product, variant or consignment identifier
 * @param stock        the current stock quantity
 */
public record RemapStockCurrentRow(String assortmentId, BigDecimal stock) {
}
