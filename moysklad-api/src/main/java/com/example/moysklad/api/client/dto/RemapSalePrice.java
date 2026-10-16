package com.example.moysklad.api.client.dto;

/**
 * A sale price entry within an assortment row of the MoySklad Remap 1.2 API.
 *
 * @param value the price value in minor currency units
 */
public record RemapSalePrice(String value) {
}
