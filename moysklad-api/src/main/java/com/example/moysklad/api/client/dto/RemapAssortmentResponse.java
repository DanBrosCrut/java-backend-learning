package com.example.moysklad.api.client.dto;

import java.util.List;

/**
 * Typed response of the MoySklad Remap 1.2 assortment endpoint.
 *
 * @param rows the assortment rows
 */
public record RemapAssortmentResponse(List<RemapAssortmentRow> rows) {
}
