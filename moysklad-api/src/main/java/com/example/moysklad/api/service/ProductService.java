package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.dto.ProductView;
import com.example.moysklad.api.mapper.ProductMapper;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service that maps the MoySklad assortment catalog to product DTOs enriched with stock data.
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
     * and maps the result to a list of product DTOs.
     * Products missing from the stock report get a stock of zero.
     *
     * @return the list of products from the MoySklad assortment with current stock
     */
    public List<ProductView> getProducts() {
        Map<String, BigDecimal> stocks = stockReportMapper.toStockMap(moyskladClient.getStockReport());
        return java.util.Optional.ofNullable(moyskladClient.getAssortment().rows())
                .map(rows -> rows.stream()
                        .map(row -> productMapper.toDto(row, stocks.get(row.id())))
                        .toList())
                .orElse(List.of());
    }
}
