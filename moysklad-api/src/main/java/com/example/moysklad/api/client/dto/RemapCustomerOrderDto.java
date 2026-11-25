package com.example.moysklad.api.client.dto;

import java.util.List;

/**
 * Customer order creation payload for the Remap API ({@code POST /entity/customerorder}).
 *
 * @param organization the selling organization
 * @param agent        the buying counterparty
 * @param positions    the ordered positions
 */
public record RemapCustomerOrderDto(
        RemapMetaDto organization,
        RemapMetaDto agent,
        List<RemapOrderPositionDto> positions) {
}
