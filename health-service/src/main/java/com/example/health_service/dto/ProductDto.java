package com.example.health_service.dto;

/**
 * DTO товара, возвращаемый эндпоинтом {@code GET /api/products}.
 *
 * @param id      идентификатор товара в МойСклад
 * @param name    наименование товара
 * @param article артикул товара
 * @param price   цена товара
 */
public record ProductDto(String id, String name, String article, String price) {
}
