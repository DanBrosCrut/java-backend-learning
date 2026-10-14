package com.example.health_service.controller;

import com.example.health_service.dto.ProductDto;
import com.example.health_service.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing the product assortment catalog.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    /**
     * Creates the controller with the product service.
     *
     * @param productService the product service
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Returns the product assortment catalog.
     *
     * @return the list of products
     */
    @GetMapping
    public List<ProductDto> getProducts() {
        return productService.getProducts();
    }
}
