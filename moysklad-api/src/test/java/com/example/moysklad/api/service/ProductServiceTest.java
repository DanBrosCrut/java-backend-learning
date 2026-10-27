package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapPriceType;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.dto.CatalogField;
import com.example.moysklad.api.dto.CatalogOptions;
import com.example.moysklad.api.dto.PageOptions;
import com.example.moysklad.api.dto.PagedResponse;
import com.example.moysklad.api.dto.ProductView;
import com.example.moysklad.api.exception.InvalidCatalogOptionException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ProductService} covering stock merging, showcase options and pagination.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SECOND_PRODUCT_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String RETAIL_PRICE = "Цена продажи";
    private static final String WHOLESALE_PRICE = "Оптовая цена";
    private static final String GROUPS = "Товары";
    private static final PageOptions FIRST_PAGE = new PageOptions(0, 20);

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
        givenAssortment(product(PRODUCT_ID, "Widget", "ART-1", GROUPS, "100000"));

        List<ProductView> products = getProducts(CatalogOptions.defaults());

        assertThat(products).hasSize(1);
        ProductView product = products.getFirst();
        assertThat(product.id()).isEqualTo(PRODUCT_ID);
        assertThat(product.name()).isEqualTo("Widget");
        assertThat(product.article()).isEqualTo("ART-1");
        assertThat(product.group()).isEqualTo(GROUPS);
        assertThat(product.price()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(product.stock()).isEqualByComparingTo(new BigDecimal("5"));
    }

    @Test
    void getProducts_missingFromStockReport_defaultsToZeroStock() {
        givenAssortment(
                product(PRODUCT_ID, "Widget", "ART-1", GROUPS, "100000"),
                product(SECOND_PRODUCT_ID, "Gadget", "ART-2", GROUPS, "250000"));

        List<ProductView> products = getProducts(CatalogOptions.defaults());

        assertThat(products).hasSize(2);
        assertThat(products)
                .filteredOn(product -> product.id().equals(SECOND_PRODUCT_ID))
                .singleElement()
                .extracting(ProductView::stock)
                .isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getProducts_requestedPriceType_usesMatchingSalePrice() {
        givenAssortment(product(PRODUCT_ID, "Widget", "ART-1", GROUPS, "100000"));

        List<ProductView> products = getProducts(
                new CatalogOptions(WHOLESALE_PRICE, List.of(), false, CatalogOptions.ALL_FIELDS));

        assertThat(products).singleElement()
                .extracting(ProductView::price)
                .isEqualTo(new BigDecimal("800.00"));
    }

    @Test
    void getProducts_unknownPriceType_throwsInvalidCatalogOptionException() {
        givenAssortment(product(PRODUCT_ID, "Widget", "ART-1", GROUPS, "100000"));

        CatalogOptions options = new CatalogOptions("Нет такой цены", List.of(), false, CatalogOptions.ALL_FIELDS);

        assertThatThrownBy(() -> getProducts(options))
                .isInstanceOf(InvalidCatalogOptionException.class)
                .hasMessageContaining("Нет такой цены");
    }

    @Test
    void getProducts_groupFilter_returnsOnlyRequestedGroup() {
        givenAssortment(
                product(PRODUCT_ID, "Widget", "ART-1", GROUPS, "100000"),
                product(SECOND_PRODUCT_ID, "Service", "SRV-1", "Услуги", "50000"));

        List<ProductView> products = getProducts(
                new CatalogOptions(null, List.of("Услуги"), false, CatalogOptions.ALL_FIELDS));

        assertThat(products).singleElement()
                .extracting(ProductView::id)
                .isEqualTo(SECOND_PRODUCT_ID);
    }

    @Test
    void getProducts_inStockOnly_hidesProductsWithoutStock() {
        givenAssortment(
                product(PRODUCT_ID, "Widget", "ART-1", GROUPS, "100000"),
                product(SECOND_PRODUCT_ID, "Gadget", "ART-2", GROUPS, "250000"));

        List<ProductView> products = getProducts(
                new CatalogOptions(null, List.of(), true, CatalogOptions.ALL_FIELDS));

        assertThat(products).singleElement()
                .extracting(ProductView::id)
                .isEqualTo(PRODUCT_ID);
    }

    @Test
    void getProducts_fieldSelection_omitsUnrequestedFields() {
        givenAssortment(product(PRODUCT_ID, "Widget", "ART-1", GROUPS, "100000"));

        List<ProductView> products = getProducts(
                new CatalogOptions(null, List.of(), false, List.of(CatalogField.NAME, CatalogField.PRICE)));

        ProductView product = products.getFirst();
        assertThat(product.name()).isEqualTo("Widget");
        assertThat(product.price()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(product.article()).isNull();
        assertThat(product.group()).isNull();
        assertThat(product.stock()).isNull();
    }

    @Test
    void getProducts_secondPage_returnsRemainingItemsAndTotalCount() {
        givenAssortment(
                product(PRODUCT_ID, "Widget", "ART-1", GROUPS, "100000"),
                product(SECOND_PRODUCT_ID, "Gadget", "ART-2", GROUPS, "250000"));

        PagedResponse<ProductView> page = productService.getProducts(
                CatalogOptions.defaults(), new PageOptions(1, 1));

        assertThat(page.items()).singleElement()
                .extracting(ProductView::id)
                .isEqualTo(SECOND_PRODUCT_ID);
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(1);
    }

    @Test
    void getProducts_pageBeyondEnd_returnsEmptyItemsWithTotal() {
        givenAssortment(product(PRODUCT_ID, "Widget", "ART-1", GROUPS, "100000"));

        PagedResponse<ProductView> page = productService.getProducts(
                CatalogOptions.defaults(), new PageOptions(5, 20));

        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isEqualTo(1);
    }

    private List<ProductView> getProducts(CatalogOptions options) {
        return productService.getProducts(options, FIRST_PAGE).items();
    }

    private void givenAssortment(RemapAssortmentRow... rows) {
        when(moyskladClient.getAssortment()).thenReturn(new RemapAssortmentResponse(List.of(rows)));
        when(moyskladClient.getStockReport()).thenReturn(STOCK_REPORT_JSON);
    }

    private static RemapAssortmentRow product(String id,
                                              String name,
                                              String article,
                                              String group,
                                              String retailPrice) {
        return new RemapAssortmentRow(id, name, article, group, List.of(
                new RemapSalePrice(new BigDecimal(retailPrice), new RemapPriceType(RETAIL_PRICE)),
                new RemapSalePrice(new BigDecimal("80000"), new RemapPriceType(WHOLESALE_PRICE))));
    }
}
