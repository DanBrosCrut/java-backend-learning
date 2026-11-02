package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapPriceType;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.dto.CatalogOptions;
import com.example.moysklad.api.dto.ProductView;
import com.example.moysklad.api.exception.InvalidCatalogOptionException;
import com.example.moysklad.api.mapper.ProductMapper;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Service that maps the MoySklad assortment catalog to product views enriched with stock data.
 */
@Service
public class ProductService {

    private final MoyskladClient moyskladClient;
    private final ProductMapper productMapper;
    private final StockReportMapper stockReportMapper;

    /**
     * Creates the service with the MoySklad client and mappers.
     *
     * @param moyskladClient    the MoySklad API client
     * @param productMapper     the Remap-to-product mapper
     * @param stockReportMapper the stock report parser
     */
    public ProductService(MoyskladClient moyskladClient,
                          ProductMapper productMapper,
                          StockReportMapper stockReportMapper) {
        this.moyskladClient = moyskladClient;
        this.productMapper = productMapper;
        this.stockReportMapper = stockReportMapper;
    }

    /**
     * Fetches the assortment and the stock report, merges them by product ID,
     * applies the showcase options and maps the result to product views.
     * Products missing from the stock report get a stock of zero.
     *
     * @param options the catalog display options; {@code null} means defaults
     * @return the products matching the options, with current stock
     * @throws InvalidCatalogOptionException when the requested price type does not exist in the assortment
     */
    public List<ProductView> getProducts(CatalogOptions options) {
        CatalogOptions effective = Optional.ofNullable(options).orElseGet(CatalogOptions::defaults);
        Map<String, BigDecimal> stocks = stockReportMapper.toStockMap(moyskladClient.getStockReport());
        List<RemapAssortmentRow> rows = Optional.ofNullable(moyskladClient.getAssortment().rows())
                .orElse(List.of());
        validatePriceType(rows, effective);

        return rows.stream()
                .filter(row -> effective.groups().isEmpty() || effective.groups().contains(row.pathName()))
                .filter(row -> !effective.inStockOnly() || stockOf(row, stocks).compareTo(BigDecimal.ZERO) > 0)
                .map(row -> productMapper.toView(row, stocks.get(row.id()), effective))
                .toList();
    }

    private static BigDecimal stockOf(RemapAssortmentRow row, Map<String, BigDecimal> stocks) {
        return Optional.ofNullable(stocks.get(row.id())).orElse(BigDecimal.ZERO);
    }

    private static void validatePriceType(List<RemapAssortmentRow> rows, CatalogOptions options) {
        String priceType = options.priceType();
        if (priceType == null) {
            return;
        }
        boolean known = rows.stream()
                .flatMap(row -> Optional.ofNullable(row.salePrices()).orElse(List.of()).stream())
                .map(RemapSalePrice::priceType)
                .filter(Objects::nonNull)
                .map(RemapPriceType::name)
                .anyMatch(priceType::equalsIgnoreCase);
        if (!known) {
            throw new InvalidCatalogOptionException(
                    "Unknown price type '" + priceType + "'. Use GET /api/products/config to list available price types");
        }
    }
}
