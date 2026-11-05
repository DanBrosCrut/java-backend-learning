package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.config.MoyskladProperties;
import com.example.moysklad.api.dto.CatalogConfigView;
import com.example.moysklad.api.dto.CatalogFilter;
import com.example.moysklad.api.dto.CatalogOptions;
import com.example.moysklad.api.dto.PageOptions;
import com.example.moysklad.api.dto.PagedResponse;
import com.example.moysklad.api.dto.ProductView;
import com.example.moysklad.api.dto.SortOptions;
import com.example.moysklad.api.exception.MoyskladApiException;
import com.example.moysklad.api.mapper.ProductMapper;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * End-to-end test of the catalog flow without Spring: the real {@link MoyskladClient} talking to a mocked
 * Remap API, the real caches and mappers. Covers chunked assortment fetching, the current stock report,
 * HIT/MISS reporting and invalidation.
 */
class CatalogFlowTest {

    private static final String BASE_URL = "https://api.moysklad.ru/api/remap/1.2";
    private static final String ASSORTMENT_URL = BASE_URL + "/entity/assortment";
    private static final String STOCK_URL = BASE_URL + "/report/stock/all/current?stockType=stock";

    private static final String FIRST_PRODUCT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SECOND_PRODUCT_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String THIRD_PRODUCT_ID = "11111111-2222-3333-4444-555555555555";

    private static final PageOptions FIRST_PAGE = new PageOptions(0, 20);

    private MockRestServiceServer server;
    private AssortmentCache assortmentCache;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        MoyskladClient client = new MoyskladClient(
                new MoyskladProperties("test-token", BASE_URL, 2), builder);
        assortmentCache = new AssortmentCache(client);
        productService = new ProductService(
                assortmentCache,
                new StockCache(client, new StockReportMapper()),
                new ProductMapper());
    }

    @Test
    void getProducts_readsEveryAssortmentChunkAndTheCurrentStockReport() {
        expectStockReport("5", "0");
        expectAssortmentPage(0, row(FIRST_PRODUCT_ID), row(SECOND_PRODUCT_ID));
        expectAssortmentPage(2, row(THIRD_PRODUCT_ID));

        CachedResult<PagedResponse<ProductView>> result = getProducts(false);

        assertThat(result.status()).isEqualTo(CacheStatus.MISS);
        assertThat(result.body().total()).isEqualTo(3);
        assertThat(result.body().items())
                .extracting(ProductView::id)
                .containsExactly(FIRST_PRODUCT_ID, SECOND_PRODUCT_ID, THIRD_PRODUCT_ID);
        assertThat(result.body().items().getFirst().stock()).isEqualByComparingTo("5");
        assertThat(result.body().items().get(1).stock()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.body().items().get(2).stock()).isEqualByComparingTo(BigDecimal.ZERO);
        server.verify();
    }

    @Test
    void getProducts_secondCall_servesCatalogAndStockFromCache() {
        expectStockReport("5", "0");
        expectAssortmentPage(0, row(FIRST_PRODUCT_ID), row(SECOND_PRODUCT_ID));
        expectAssortmentPage(2, row(THIRD_PRODUCT_ID));

        getProducts(false);
        CachedResult<PagedResponse<ProductView>> second = getProducts(false);

        assertThat(second.status()).isEqualTo(CacheStatus.HIT);
        assertThat(second.body().total()).isEqualTo(3);
        server.verify();
    }

    @Test
    void getProducts_invalidateTrue_reloadsBothCaches() {
        expectStockReport("5");
        expectAssortmentPage(0, row(FIRST_PRODUCT_ID), row(SECOND_PRODUCT_ID));
        expectAssortmentPage(2, row(THIRD_PRODUCT_ID));
        expectStockReport("9");
        expectAssortmentPage(0, row(FIRST_PRODUCT_ID));

        getProducts(false);
        CachedResult<PagedResponse<ProductView>> reloaded = getProducts(true);

        assertThat(reloaded.status()).isEqualTo(CacheStatus.MISS);
        assertThat(reloaded.body().items()).singleElement()
                .satisfies(product -> {
                    assertThat(product.id()).isEqualTo(FIRST_PRODUCT_ID);
                    assertThat(product.stock()).isEqualByComparingTo("9");
                });
        server.verify();
    }

    @Test
    void getProducts_failingChunk_isNotCachedAndNextCallRetries() {
        server.expect(requestTo(STOCK_URL))
                .andRespond(withSuccess(stockJson("5"), MediaType.APPLICATION_JSON));
        expectAssortmentPage(0, row(FIRST_PRODUCT_ID), row(SECOND_PRODUCT_ID));
        server.expect(requestTo(ASSORTMENT_URL + "?limit=2&offset=2"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> getProducts(false))
                .isInstanceOf(MoyskladApiException.class)
                .hasMessageContaining("offset 2");

        server.reset();
        expectAssortmentPage(0, row(FIRST_PRODUCT_ID), row(SECOND_PRODUCT_ID));
        expectAssortmentPage(2, row(THIRD_PRODUCT_ID));

        CachedResult<PagedResponse<ProductView>> retried = getProducts(false);

        assertThat(retried.status()).isEqualTo(CacheStatus.MISS);
        assertThat(retried.body().total()).isEqualTo(3);
        server.verify();
    }

    @Test
    void getConfigAfterCatalogWarmUp_reusesTheCachedAssortment() {
        expectStockReport("5");
        expectAssortmentPage(0, row(FIRST_PRODUCT_ID));

        getProducts(false);
        CatalogConfigService configService = new CatalogConfigService(assortmentCache);
        CachedResult<CatalogConfigView> config = configService.describe(false);

        assertThat(config.status()).isEqualTo(CacheStatus.HIT);
        assertThat(config.body().groups()).containsExactly("Товары");
        server.verify();
    }

    private CachedResult<PagedResponse<ProductView>> getProducts(boolean invalidate) {
        return productService.getProducts(
                CatalogOptions.defaults(), CatalogFilter.none(), SortOptions.natural(), FIRST_PAGE, invalidate);
    }

    private void expectAssortmentPage(int offset, String... rows) {
        server.expect(requestTo(ASSORTMENT_URL + "?limit=2&offset=" + offset))
                .andRespond(withSuccess("{\"rows\":[" + String.join(",", rows) + "]}", MediaType.APPLICATION_JSON));
    }

    private void expectStockReport(String... stocks) {
        server.expect(requestTo(STOCK_URL))
                .andRespond(withSuccess(stockJson(stocks), MediaType.APPLICATION_JSON));
    }

    private static String stockJson(String... stocks) {
        String[] ids = {FIRST_PRODUCT_ID, SECOND_PRODUCT_ID};
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < stocks.length; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"assortmentId\":\"")
                    .append(ids[index])
                    .append("\",\"stock\":")
                    .append(stocks[index])
                    .append('}');
        }
        return json.append(']').toString();
    }

    private static String row(String id) {
        return """
                {"id":"%s","name":"Product %s","article":"ART-%s","pathName":"Товары","salePrices":[]}
                """.formatted(id, id, id).trim();
    }
}
