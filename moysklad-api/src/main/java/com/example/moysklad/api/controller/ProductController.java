package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.ProductDto;
import com.example.moysklad.api.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing the product assortment catalog.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product assortment catalog from MoySklad")
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
    @Operation(summary = "Get product assortment", description = "Returns the full product catalog with current stock levels")
    @GetMapping
    public List<ProductDto> getProducts() {
        return productService.getProducts();
    }
}
