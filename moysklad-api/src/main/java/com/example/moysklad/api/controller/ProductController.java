package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.CatalogConfigView;
import com.example.moysklad.api.dto.CatalogOptions;
import com.example.moysklad.api.dto.PageOptions;
import com.example.moysklad.api.dto.PagedResponse;
import com.example.moysklad.api.dto.ProductView;
import com.example.moysklad.api.service.CatalogConfigService;
import com.example.moysklad.api.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the product assortment catalog and its showcase options.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product assortment catalog and showcase display options from MoySklad")
public class ProductController {

    private final ProductService productService;
    private final CatalogConfigService catalogConfigService;

    /**
     * Creates the controller with the product service and the catalog configuration service.
     *
     * @param productService       the product service
     * @param catalogConfigService the catalog options parser and descriptor
     */
    public ProductController(ProductService productService, CatalogConfigService catalogConfigService) {
        this.productService = productService;
        this.catalogConfigService = catalogConfigService;
    }

    /**
     * Returns one page of the product assortment catalog configured like the MoySklad showcase.
     *
     * @param priceType   the price type to show; empty means the first sale price
     * @param groups      comma-separated product group names; empty means all groups
     * @param fields      comma-separated fields to include; empty means all fields
     * @param inStockOnly whether to hide products with zero stock
     * @param page        the zero-based page index
     * @param size        the page size (1..100)
     * @return one configured page of products with the total number of matches
     */
    @Operation(summary = "Get product assortment",
            description = "Returns a page of the product catalog with current stock levels, filtered and projected according to the showcase options")
    @GetMapping
    public PagedResponse<ProductView> getProducts(
            @Parameter(description = "Price type name, e.g. 'Цена продажи'. Empty means the first sale price", example = "Цена продажи")
            @RequestParam(required = false) String priceType,
            @Parameter(description = "Comma-separated product group names. Empty means all groups", example = "Товары")
            @RequestParam(required = false) String groups,
            @Parameter(description = "Comma-separated fields: name, article, group, price, stock. Empty means all", example = "name,price,stock")
            @RequestParam(required = false) String fields,
            @Parameter(description = "Hide products with zero stock", example = "false")
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, from 1 to 100", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        CatalogOptions options = catalogConfigService.parse(priceType, groups, fields, inStockOnly);
        PageOptions pageOptions = catalogConfigService.parsePage(page, size);
        return productService.getProducts(options, pageOptions);
    }

    /**
     * Returns the catalog options available for the current MoySklad account.
     *
     * @return available price types, groups, fields and default options
     */
    @Operation(summary = "Get catalog configuration",
            description = "Returns the price types and product groups present in MoySklad, the supported fields and the default options")
    @GetMapping("/config")
    public CatalogConfigView getConfig() {
        return catalogConfigService.describe();
    }
}
