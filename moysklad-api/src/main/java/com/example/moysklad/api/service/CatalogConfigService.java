package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapPriceType;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.dto.CatalogConfigView;
import com.example.moysklad.api.dto.CatalogField;
import com.example.moysklad.api.dto.CatalogOptions;
import com.example.moysklad.api.dto.PageOptions;
import com.example.moysklad.api.exception.InvalidCatalogOptionException;
import com.example.moysklad.api.exception.InvalidPaginationException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Parses and validates catalog display options and describes the options available in MoySklad.
 */
@Service
public class CatalogConfigService {

    /**
     * Page size used when the client does not pass the {@code size} parameter.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Upper bound of the {@code size} parameter, protecting the API from oversized responses.
     */
    public static final int MAX_PAGE_SIZE = 100;

    private final MoyskladClient moyskladClient;

    /**
     * Creates the service with the MoySklad client used to discover available options.
     *
     * @param moyskladClient the MoySklad API client
     */
    public CatalogConfigService(MoyskladClient moyskladClient) {
        this.moyskladClient = moyskladClient;
    }

    /**
     * Parses raw query parameters into validated catalog options.
     *
     * @param priceType  the requested price type name, may be {@code null}
     * @param groups     comma-separated group names, may be {@code null}
     * @param fields     comma-separated field names, may be {@code null}
     * @param inStockOnly whether products without stock must be hidden
     * @return the resolved catalog options
     * @throws InvalidCatalogOptionException when a field name is unknown
     */
    public CatalogOptions parse(String priceType, String groups, String fields, boolean inStockOnly) {
        return new CatalogOptions(priceType, split(groups), inStockOnly, parseFields(fields));
    }

    /**
     * Validates the pagination parameters of a catalog request.
     *
     * @param page the zero-based page index
     * @param size the requested page size
     * @return the validated pagination parameters
     * @throws InvalidPaginationException when the page is negative or the size is out of the allowed range
     */
    public PageOptions parsePage(int page, int size) {
        if (page < 0) {
            throw new InvalidPaginationException("Parameter 'page' must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Parameter 'size' must be between 1 and " + MAX_PAGE_SIZE);
        }
        return new PageOptions(page, size);
    }

    /**
     * Describes the showcase options available for the current MoySklad account:
     * price types and product groups present in the assortment, supported fields and defaults.
     *
     * @return the available catalog configuration
     */
    public CatalogConfigView describe() {
        List<RemapAssortmentRow> rows = Optional.ofNullable(moyskladClient.getAssortment().rows())
                .orElse(List.of());

        List<String> priceTypes = rows.stream()
                .flatMap(row -> Optional.ofNullable(row.salePrices()).orElse(List.of()).stream())
                .map(RemapSalePrice::priceType)
                .filter(Objects::nonNull)
                .map(RemapPriceType::name)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted()
                .toList();

        List<String> groups = rows.stream()
                .map(RemapAssortmentRow::pathName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted()
                .toList();

        List<String> fields = Arrays.stream(CatalogField.values())
                .map(CatalogField::queryName)
                .toList();

        CatalogOptions defaults = new CatalogOptions(
                priceTypes.isEmpty() ? null : priceTypes.getFirst(),
                groups,
                false,
                CatalogOptions.ALL_FIELDS);

        return new CatalogConfigView(priceTypes, groups, fields, defaults);
    }

    private static List<CatalogField> parseFields(String fields) {
        if (fields == null || fields.isBlank()) {
            return CatalogOptions.ALL_FIELDS;
        }
        return split(fields).stream()
                .map(name -> CatalogField.fromQueryName(name)
                        .orElseThrow(() -> new InvalidCatalogOptionException(
                                "Unknown field '" + name + "'. Allowed fields: " + CatalogField.allQueryNames())))
                .distinct()
                .toList();
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .distinct()
                .toList();
    }
}
