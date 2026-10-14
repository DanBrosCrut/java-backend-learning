package com.example.health_service.service;

import com.example.health_service.client.MoyskladClient;
import com.example.health_service.dto.ProductDto;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that maps the MoySklad assortment catalog to product DTOs.
 */
@Service
public class ProductService {

    private final MoyskladClient moyskladClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates the service with the MoySklad client and JSON mapper.
     *
     * @param moyskladClient the MoySklad API client
     * @param objectMapper   the JSON mapper
     */
    public ProductService(MoyskladClient moyskladClient, ObjectMapper objectMapper) {
        this.moyskladClient = moyskladClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches the assortment and maps it to a list of product DTOs.
     *
     * @return the list of products from the MoySklad assortment
     */
    public List<ProductDto> getProducts() {
        String json = moyskladClient.getAssortment();
        List<ProductDto> products = new ArrayList<>();
        try {
            JsonNode rows = objectMapper.readTree(json).path("rows");
            for (JsonNode row : rows) {
                products.add(new ProductDto(
                        row.path("id").asText(),
                        row.path("name").asText(),
                        row.path("article").asText(),
                        extractPrice(row)));
            }
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to parse MoySklad assortment response", e);
        }
        return products;
    }

    private String extractPrice(JsonNode row) {
        JsonNode salePrices = row.path("salePrices");
        if (salePrices.isArray() && !salePrices.isEmpty()) {
            JsonNode value = salePrices.get(0).path("value");
            if (value.isNumber()) {
                return BigDecimal.valueOf(value.asLong()).movePointLeft(2).toPlainString();
            }
        }
        return null;
    }
}
