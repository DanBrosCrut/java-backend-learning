package com.example.moysklad.api.controller;

import com.example.moysklad.api.dto.CatalogConfigView;
import com.example.moysklad.api.dto.CatalogFilter;
import com.example.moysklad.api.dto.CatalogOptions;
import com.example.moysklad.api.dto.PageOptions;
import com.example.moysklad.api.dto.PagedResponse;
import com.example.moysklad.api.dto.ProductView;
import com.example.moysklad.api.dto.SortOptions;
import com.example.moysklad.api.service.CacheStatus;
import com.example.moysklad.api.service.CachedResult;
import com.example.moysklad.api.service.CatalogConfigService;
import com.example.moysklad.api.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link ProductController}: the {@code X-Cache} header and the invalidation flag.
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private static final String CACHE_HEADER = "X-Cache";
    private static final String FORCE_REFRESH_HEADER = "X-Force-Refresh";

    @Mock
    private ProductService productService;

    @Mock
    private CatalogConfigService catalogConfigService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService, catalogConfigService))
                .build();
    }

    @Test
    void getProducts_cacheHit_returnsHitHeaderAndPage() throws Exception {
        givenParsedOptions();
        when(productService.getProducts(any(), any(), any(), any(), eq(false)))
                .thenReturn(new CachedResult<>(pageOf(product()), CacheStatus.HIT));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(header().string(CACHE_HEADER, "HIT"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Widget"));
    }

    @Test
    void getProducts_cacheMiss_returnsMissHeader() throws Exception {
        givenParsedOptions();
        when(productService.getProducts(any(), any(), any(), any(), eq(false)))
                .thenReturn(new CachedResult<>(pageOf(product()), CacheStatus.MISS));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(header().string(CACHE_HEADER, "MISS"));
    }

    @Test
    void getProducts_invalidateTrue_isPassedToTheService() throws Exception {
        givenParsedOptions();
        when(productService.getProducts(any(), any(), any(), any(), eq(true)))
                .thenReturn(new CachedResult<>(pageOf(product()), CacheStatus.MISS));

        mockMvc.perform(get("/api/products").param("invalidate", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string(CACHE_HEADER, "MISS"));

        verify(productService).getProducts(any(), any(), any(), any(), eq(true));
    }

    @Test
    void getProducts_forceRefreshHeader_isPassedToTheService() throws Exception {
        givenParsedOptions();
        when(productService.getProducts(any(), any(), any(), any(), eq(true)))
                .thenReturn(new CachedResult<>(pageOf(product()), CacheStatus.MISS));

        mockMvc.perform(get("/api/products").header(FORCE_REFRESH_HEADER, "true"))
                .andExpect(status().isOk())
                .andExpect(header().string(CACHE_HEADER, "MISS"));

        verify(productService).getProducts(any(), any(), any(), any(), eq(true));
    }

    @Test
    void getConfig_returnsCacheHeader() throws Exception {
        when(catalogConfigService.describe(false))
                .thenReturn(new CachedResult<>(
                        new CatalogConfigView(List.of("Цена продажи"), List.of("Товары"),
                                List.of("name"), CatalogOptions.defaults()),
                        CacheStatus.HIT));

        mockMvc.perform(get("/api/products/config"))
                .andExpect(status().isOk())
                .andExpect(header().string(CACHE_HEADER, "HIT"))
                .andExpect(jsonPath("$.groups[0]").value("Товары"));
    }

    private void givenParsedOptions() {
        when(catalogConfigService.parse(null, null, null, false)).thenReturn(CatalogOptions.defaults());
        when(catalogConfigService.parseFilter(null)).thenReturn(CatalogFilter.none());
        when(catalogConfigService.parseSort(null, "asc")).thenReturn(SortOptions.natural());
        when(catalogConfigService.parsePage(0, 20)).thenReturn(new PageOptions(0, 20));
    }

    private static ProductView product() {
        return new ProductView("id-1", "Widget", "ART-1", "Товары", new BigDecimal("100.00"), BigDecimal.ONE);
    }

    private static PagedResponse<ProductView> pageOf(ProductView product) {
        return PagedResponse.of(List.of(product), 0, 20);
    }
}
