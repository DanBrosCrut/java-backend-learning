package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.dto.ProductDto;
import com.example.moysklad.api.mapper.ProductMapper;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Happy-path tests for {@link ProductService}.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SECOND_PRODUCT_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    private static final String STOCK_REPORT_JSON = """
            {
              "rows": [
                {
                  "assortment": {
                    "meta": {
                      "href": "https://api.moysklad.ru/api/remap/1.2/entity/product/123e4567-e89b-12d3-a456-426614174000"
                    }
                  },
                  "quantity": 5
                }
              ]
            }
            """;

    @Mock
    private MoyskladClient moyskladClient;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                moyskladClient,
                new ProductMapper(),
                new StockReportMapper(new ObjectMapper()));
    }

    @Test
    void getProducts_populatesStockFromStockReport() {
        RemapAssortmentResponse assortment = new RemapAssortmentResponse(List.of(
                new RemapAssortmentRow(PRODUCT_ID, "Widget", "ART-1", List.of(new RemapSalePrice("100000")))));
        when(moyskladClient.getAssortment()).thenReturn(assortment);
        when(moyskladClient.getStockReport()).thenReturn(STOCK_REPORT_JSON);

        List<ProductDto> products = productService.getProducts();

        assertThat(products).hasSize(1);
        ProductDto product = products.get(0);
        assertThat(product.id()).isEqualTo(PRODUCT_ID);
        assertThat(product.name()).isEqualTo("Widget");
        assertThat(product.article()).isEqualTo("ART-1");
        assertThat(product.price()).isEqualTo("1000.00");
        assertThat(product.stock()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void getProducts_missingFromStockReport_defaultsToZeroStock() {
        RemapAssortmentResponse assortment = new RemapAssortmentResponse(List.of(
                new RemapAssortmentRow(PRODUCT_ID, "Widget", "ART-1", List.of(new RemapSalePrice("100000"))),
                new RemapAssortmentRow(SECOND_PRODUCT_ID, "Gadget", "ART-2", List.of(new RemapSalePrice("250000")))));
        when(moyskladClient.getAssortment()).thenReturn(assortment);
        when(moyskladClient.getStockReport()).thenReturn(STOCK_REPORT_JSON);

        List<ProductDto> products = productService.getProducts();

        assertThat(products).hasSize(2);
        assertThat(products)
                .filteredOn(p -> p.id().equals(PRODUCT_ID))
                .singleElement()
                .extracting(ProductDto::stock)
                .isEqualTo(BigDecimal.valueOf(5));
        assertThat(products)
                .filteredOn(p -> p.id().equals(SECOND_PRODUCT_ID))
                .singleElement()
                .extracting(ProductDto::stock)
                .isEqualTo(BigDecimal.ZERO);
    }
}
