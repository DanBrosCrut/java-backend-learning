package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.CatalogConfigView;
import com.example.moysklad.api.dto.CatalogFilter;
import com.example.moysklad.api.dto.CatalogOptions;
import com.example.moysklad.api.dto.PageOptions;
import com.example.moysklad.api.dto.PagedResponse;
import com.example.moysklad.api.dto.ProductView;
import com.example.moysklad.api.dto.SortOptions;
import com.example.moysklad.api.service.CachedResult;
import com.example.moysklad.api.service.CatalogConfigService;
import com.example.moysklad.api.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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

    /**
     * Name of the response header reporting whether the catalog snapshot came from the in-memory cache.
     */
    public static final String CACHE_HEADER = "X-Cache";

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
     * @param q           case-insensitive text matched against product name and article
     * @param fields      comma-separated fields to include; empty means all fields
     * @param inStockOnly whether to hide products with zero stock
     * @param sortBy      the field to sort by; empty keeps the MoySklad order
     * @param order       the sort direction: asc or desc
     * @param page        the zero-based page index
     * @param size        the page size (1..100)
     * @param invalidate  whether to force a reload of the cached assortment snapshot
     * @return one configured page of products with the total number of matches and the {@code X-Cache} header
     */
    @Operation(summary = "Get product assortment",
            description = """
                    Returns a page of the product catalog with current stock levels, filtered and projected \
                    according to the showcase options. The assortment snapshot is served from an in-memory cache: \
                    the X-Cache response header is HIT when it came from the cache and MISS when it was reloaded \
                    from MoySklad. Pass invalidate=true to force a reload of the cache.""")
    @GetMapping
    public ResponseEntity<PagedResponse<ProductView>> getProducts(
            @Parameter(description = "Price type name, e.g. 'Цена продажи'. Empty means the first sale price", example = "Цена продажи")
            @RequestParam(required = false) String priceType,
            @Parameter(description = "Comma-separated product group names. Empty means all groups", example = "Товары")
            @RequestParam(required = false) String groups,
            @Parameter(description = "Case-insensitive text matched against product name and article (max 100 chars)", example = "машин")
            @RequestParam(required = false) String q,
            @Parameter(description = "Comma-separated fields: name, article, group, price, stock. Empty means all", example = "name,price,stock")
            @RequestParam(required = false) String fields,
            @Parameter(description = "Hide products with zero stock", example = "false")
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @Parameter(description = "Field to sort by: name, article, group, price, stock. Empty keeps the MoySklad order", example = "price")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction: asc or desc", example = "asc")
            @RequestParam(defaultValue = "asc") String order,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, from 1 to 100", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Drop the cached assortment and stock snapshots and reload them from MoySklad", example = "false")
            @RequestParam(defaultValue = "false") boolean invalidate) {
        CatalogOptions options = catalogConfigService.parse(priceType, groups, fields, inStockOnly);
        CatalogFilter filter = catalogConfigService.parseFilter(q);
        SortOptions sort = catalogConfigService.parseSort(sortBy, order);
        PageOptions pageOptions = catalogConfigService.parsePage(page, size);
        CachedResult<PagedResponse<ProductView>> result =
                productService.getProducts(options, filter, sort, pageOptions, invalidate);
        return withCacheHeader(result);
    }

    /**
     * Returns the catalog options available for the current MoySklad account.
     *
     * @param invalidate whether to force a reload of the cached assortment snapshot
     * @return available price types, groups, fields and default options with the {@code X-Cache} header
     */
    @Operation(summary = "Get catalog configuration",
            description = """
                    Returns the price types and product groups present in MoySklad, the supported fields and the \
                    default options. Served from the same in-memory assortment cache as the catalog; the X-Cache \
                    header reports HIT or MISS, and invalidate=true forces a reload.""")
    @GetMapping("/config")
    public ResponseEntity<CatalogConfigView> getConfig(
            @Parameter(description = "Drop the cached assortment snapshot and reload it from MoySklad", example = "false")
            @RequestParam(defaultValue = "false") boolean invalidate) {
        return withCacheHeader(catalogConfigService.describe(invalidate));
    }

    private static <T> ResponseEntity<T> withCacheHeader(CachedResult<T> result) {
        return ResponseEntity.ok()
                .header(CACHE_HEADER, result.status().name())
                .body(result.body());
    }
}
