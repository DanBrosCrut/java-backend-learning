package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapPriceType;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.dto.CatalogFilter;
import com.example.moysklad.api.dto.CatalogOptions;
import com.example.moysklad.api.dto.PageOptions;
import com.example.moysklad.api.dto.PagedResponse;
import com.example.moysklad.api.dto.ProductView;
import com.example.moysklad.api.dto.SortOptions;
import com.example.moysklad.api.dto.SortOrder;
import com.example.moysklad.api.exception.InvalidCatalogOptionException;
import com.example.moysklad.api.mapper.ProductMapper;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
     * applies the showcase options and maps the result to a single page of product views.
     * Products missing from the stock report get a stock of zero.
     *
     * @param options the catalog display options; {@code null} means defaults
     * @param filter the free-text filter matched against name and article
     * @param sort the sorting of the resulting page; {@code null} means the natural order
     * @param pageOptions the pagination parameters
     * @return one page of products matching the options and filter, sorted as requested,
     *         with the total number of matches
     * @throws InvalidCatalogOptionException when the requested price type does not exist in the assortment
     */
    public PagedResponse<ProductView> getProducts(CatalogOptions options,
                                                  CatalogFilter filter,
                                                  SortOptions sort,
                                                  PageOptions pageOptions) {
        CatalogOptions effective = Optional.ofNullable(options).orElseGet(CatalogOptions::defaults);
        CatalogFilter effectiveFilter = Optional.ofNullable(filter).orElseGet(CatalogFilter::none);
        SortOptions effectiveSort = Optional.ofNullable(sort).orElseGet(SortOptions::natural);
        Map<String, BigDecimal> stocks = stockReportMapper.toStockMap(moyskladClient.getStockReport());
        List<RemapAssortmentRow> rows = Optional.ofNullable(moyskladClient.getAssortment().rows())
                .orElse(List.of());
        validatePriceType(rows, effective);

        List<ProductView> products = rows.stream()
                .filter(row -> effective.groups().isEmpty() || effective.groups().contains(row.pathName()))
                .filter(row -> matchesQuery(row, effectiveFilter))
                .filter(row -> !effective.inStockOnly() || stockOf(row, stocks).compareTo(BigDecimal.ZERO) > 0)
                .map(row -> productMapper.toView(row, stocks.get(row.id()), effective))
                .sorted(comparatorFor(effectiveSort))
                .toList();

        return PagedResponse.of(products, pageOptions.page(), pageOptions.size());
    }

    private static boolean matchesQuery(RemapAssortmentRow row, CatalogFilter filter) {
        if (filter.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(row.name(), filter.query())
                || containsIgnoreCase(row.article(), filter.query());
    }

    private static boolean containsIgnoreCase(String value, String lowerCaseQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    private static Comparator<ProductView> comparatorFor(SortOptions sort) {
        if (sort.isEmpty()) {
            return (left, right) -> 0;
        }
        return (left, right) -> compareBy(sort, left, right);
    }

    private static int compareBy(SortOptions sort, ProductView left, ProductView right) {
        return switch (sort.field()) {
            case NAME -> compareValues(left.name(), right.name(), String.CASE_INSENSITIVE_ORDER, sort.order());
            case ARTICLE -> compareValues(left.article(), right.article(), String.CASE_INSENSITIVE_ORDER, sort.order());
            case GROUP -> compareValues(left.group(), right.group(), String.CASE_INSENSITIVE_ORDER, sort.order());
            case PRICE -> compareValues(left.price(), right.price(), Comparator.naturalOrder(), sort.order());
            case STOCK -> compareValues(left.stock(), right.stock(), Comparator.naturalOrder(), sort.order());
        };
    }

    private static <T> int compareValues(T left, T right, Comparator<T> comparator, SortOrder order) {
        if (left == null || right == null) {
            return left == null ? (right == null ? 0 : 1) : -1;
        }
        int result = comparator.compare(left, right);
        return order == SortOrder.DESC ? -result : result;
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
