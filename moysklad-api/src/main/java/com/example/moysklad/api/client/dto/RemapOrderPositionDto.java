package com.example.moysklad.api.client.dto;

import java.math.BigDecimal;

/**
 * One position of a customer order payload for the Remap API.
 *
 * @param quantity   the ordered quantity
 * @param price      the price per unit in the order currency
 * @param assortment the meta reference to the product
 */
public record RemapOrderPositionDto(
        BigDecimal quantity,
        BigDecimal price,
        RemapMetaDto assortment) {
}
