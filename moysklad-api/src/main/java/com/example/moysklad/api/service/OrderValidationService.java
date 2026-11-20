package com.example.moysklad.api.service;

import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.dto.AvailabilityCheckRequest;
import com.example.moysklad.api.dto.AvailabilityCheckResponse;
import com.example.moysklad.api.dto.AvailabilityCheckResponse.MissingPosition;
import com.example.moysklad.api.dto.OrderValidationRequest;
import com.example.moysklad.api.dto.OrderValidationResponse;
import com.example.moysklad.api.dto.OrderValidationResponse.AlternativeProduct;
import com.example.moysklad.api.dto.OrderValidationResponse.ValidationError;
import com.example.moysklad.api.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Validates order positions against the current stock before an order is placed.
 * <p>
 * Validation reuses {@link AvailabilityService} for the stock check and enriches every missing position
 * with a human-readable message and up to three in-stock alternatives. Products from the same group are
 * suggested first, then products from other groups. No order is created.
 */
@Service
public class OrderValidationService {

    /**
     * Maximum number of alternative products suggested per missing position.
     */
    public static final int MAX_ALTERNATIVES = 3;

    private final AvailabilityService availabilityService;
    private final AssortmentCache assortmentCache;
    private final StockCache stockCache;
    private final ProductMapper productMapper;

    /**
     * Creates the service with the availability check and the catalog caches.
     *
     * @param availabilityService the stock availability check
     * @param assortmentCache     the in-memory assortment snapshot used for names, groups and prices
     * @param stockCache          the in-memory stock report used to filter and rank alternatives
     * @param productMapper       the mapper extracting sale prices from Remap rows
     */
    public OrderValidationService(AvailabilityService availabilityService,
                                  AssortmentCache assortmentCache,
                                  StockCache stockCache,
                                  ProductMapper productMapper) {
        this.availabilityService = availabilityService;
        this.assortmentCache = assortmentCache;
        this.stockCache = stockCache;
        this.productMapper = productMapper;
    }

    /**
     * Checks whether all requested positions are covered by the current stock.
     *
     * @param request the requested positions
     * @return {@code valid = true} when everything is available; otherwise the list of errors
     *         with requested, available and alternative products
     */
    public OrderValidationResponse validate(OrderValidationRequest request) {
        AvailabilityCheckResponse availability = availabilityService.checkAvailability(
                toAvailabilityRequest(request));
        if (availability.available()) {
            return new OrderValidationResponse(true, List.of());
        }

        Map<String, BigDecimal> stocks = stockCache.get(false).body();
        List<RemapAssortmentRow> rows = Optional.ofNullable(assortmentCache.get(false).body().rows())
                .orElseGet(List::of);
        List<ValidationError> errors = availability.missing().stream()
                .map(missing -> toValidationError(missing, rows, stocks))
                .toList();
        return new OrderValidationResponse(false, errors);
    }

    private static AvailabilityCheckRequest toAvailabilityRequest(OrderValidationRequest request) {
        Map<String, Integer> quantities = request.positions().stream()
                .collect(Collectors.toMap(
                        OrderValidationRequest.Position::productId,
                        OrderValidationRequest.Position::quantity,
                        Integer::sum,
                        LinkedHashMap::new));
        List<AvailabilityCheckRequest.Position> positions = quantities.entrySet().stream()
                .map(entry -> new AvailabilityCheckRequest.Position(entry.getKey(), entry.getValue()))
                .toList();
        return new AvailabilityCheckRequest(positions);
    }

    private ValidationError toValidationError(MissingPosition missing,
                                              List<RemapAssortmentRow> rows,
                                              Map<String, BigDecimal> stocks) {
        String name = rows.stream()
                .filter(row -> missing.id().equals(row.id()))
                .map(RemapAssortmentRow::name)
                .findFirst()
                .orElse(null);
        return new ValidationError(
                missing.id(),
                missing.requested(),
                missing.available(),
                messageFor(missing, name),
                alternativesFor(missing.id(), rows, stocks));
    }

    private static String messageFor(MissingPosition missing, String name) {
        String subject = name == null
                ? "product " + missing.id()
                : "'" + name + "'";
        return "Insufficient stock for " + subject
                + ": requested " + missing.requested()
                + ", available " + missing.available();
    }

    private List<AlternativeProduct> alternativesFor(String productId,
                                                     List<RemapAssortmentRow> rows,
                                                     Map<String, BigDecimal> stocks) {
        String group = rows.stream()
                .filter(row -> productId.equals(row.id()))
                .map(RemapAssortmentRow::pathName)
                .findFirst()
                .orElse(null);
        Comparator<RemapAssortmentRow> sameGroupFirst = Comparator.comparingInt(
                row -> group != null && group.equals(row.pathName()) ? 0 : 1);
        return rows.stream()
                .filter(row -> !productId.equals(row.id()))
                .filter(row -> stockOf(row, stocks).compareTo(BigDecimal.ZERO) > 0)
                .sorted(sameGroupFirst.thenComparing(
                        row -> Optional.ofNullable(row.name()).orElse(""),
                        String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_ALTERNATIVES)
                .map(row -> toAlternative(row, stocks))
                .toList();
    }

    private AlternativeProduct toAlternative(RemapAssortmentRow row, Map<String, BigDecimal> stocks) {
        return new AlternativeProduct(
                row.id(),
                row.name(),
                Optional.ofNullable(row.article()).orElse(""),
                productMapper.extractPrice(row.salePrices(), null).orElse(null),
                stockOf(row, stocks));
    }

    private static BigDecimal stockOf(RemapAssortmentRow row, Map<String, BigDecimal> stocks) {
        return Optional.ofNullable(stocks.get(row.id())).orElse(BigDecimal.ZERO);
    }
}
