package com.example.moysklad.api.client.dto;

import java.util.List;

/**
 * Typed response of the MoySklad Remap 1.2 stock report endpoint ({@code /report/stock/all}).
 *
 * @param rows the stock report rows
 */
public record RemapStockReportResponse(List<RemapStockRow> rows) {
}
