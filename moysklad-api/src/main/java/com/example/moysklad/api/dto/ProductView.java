package com.example.moysklad.api.dto;

import java.math.BigDecimal;

/**
 * DTO товара, возвращаемый эндпоинтом {@code GET /api/products}.
 *
 * @param id      идентификатор товара в МойСклад
 * @param name    наименование товара
 * @param article артикул товара
 * @param price   цена товара
 * @param stock   количество товара на складе (0, если нет записи в отчёте остатков)
 */
public record ProductDto(String id, String name, String article, String price, BigDecimal stock) {
}
