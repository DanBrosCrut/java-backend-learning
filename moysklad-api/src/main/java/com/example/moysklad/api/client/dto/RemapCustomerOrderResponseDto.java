package com.example.moysklad.api.client.dto;

import java.math.BigDecimal;

/**
 * Created customer order returned by the Remap API.
 *
 * @param id   the order UUID in MoySklad
 * @param name the order number shown in the MoySklad UI
 * @param sum  the order total in the order currency
 */
public record RemapCustomerOrderResponseDto(
        String id,
        String name,
        BigDecimal sum) {
}
