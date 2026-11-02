package com.example.moysklad.api.client.dto;

/**
 * Price type reference of a sale price in the MoySklad Remap 1.2 API.
 *
 * @param name the human-readable price type name, e.g. {@code Цена продажи}
 */
public record RemapPriceType(String name) {
}
