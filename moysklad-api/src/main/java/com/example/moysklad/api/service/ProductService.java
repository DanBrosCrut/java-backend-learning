package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.dto.ProductDto;
import com.example.moysklad.api.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that maps the MoySklad assortment catalog to product DTOs.
 */
@Service
public class ProductService {

    private final MoyskladClient moyskladClient;
    private final ProductMapper productMapper;

    /**
     * Creates the service with the MoySklad client and product mapper.
     *
     * @param moyskladClient the MoySklad API client
     * @param productMapper  the Remap-to-product mapper
     */
    public ProductService(MoyskladClient moyskladClient, ProductMapper productMapper) {
        this.moyskladClient = moyskladClient;
        this.productMapper = productMapper;
    }

    /**
     * Fetches the assortment and maps it to a list of product DTOs.
     *
     * @return the list of products from the MoySklad assortment
     */
    public List<ProductDto> getProducts() {
        return moyskladClient.getAssortment().rows().stream()
                .map(productMapper::toDto)
                .toList();
    }
}
