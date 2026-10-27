package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapPriceType;
import com.example.moysklad.api.client.dto.RemapSalePrice;
import com.example.moysklad.api.dto.CatalogConfigView;
import com.example.moysklad.api.dto.CatalogField;
import com.example.moysklad.api.dto.CatalogOptions;
import com.example.moysklad.api.dto.PageOptions;
import com.example.moysklad.api.exception.InvalidCatalogOptionException;
import com.example.moysklad.api.exception.InvalidPaginationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CatalogConfigService}: query parsing, pagination validation and option discovery.
 */
@ExtendWith(MockitoExtension.class)
class CatalogConfigServiceTest {

    @Mock
    private MoyskladClient moyskladClient;

    private CatalogConfigService catalogConfigService;

    @BeforeEach
    void setUp() {
        catalogConfigService = new CatalogConfigService(moyskladClient);
    }

    @Test
    void parse_emptyParameters_returnsDefaults() {
        CatalogOptions options = catalogConfigService.parse(null, null, null, false);

        assertThat(options.priceType()).isNull();
        assertThat(options.groups()).isEmpty();
        assertThat(options.fields()).containsExactlyElementsOf(CatalogOptions.ALL_FIELDS);
        assertThat(options.inStockOnly()).isFalse();
    }

    @Test
    void parse_commaSeparatedValues_areTrimmedAndDeduplicated() {
        CatalogOptions options = catalogConfigService.parse(" Цена продажи ", "Товары, Услуги,Товары", " name , price ", true);

        assertThat(options.priceType()).isEqualTo("Цена продажи");
        assertThat(options.groups()).containsExactly("Товары", "Услуги");
        assertThat(options.fields()).containsExactly(CatalogField.NAME, CatalogField.PRICE);
        assertThat(options.inStockOnly()).isTrue();
    }

    @Test
    void parse_unknownField_throwsInvalidCatalogOptionException() {
        assertThatThrownBy(() -> catalogConfigService.parse(null, null, "name,unknown", false))
                .isInstanceOf(InvalidCatalogOptionException.class)
                .hasMessageContaining("unknown")
                .hasMessageContaining("Allowed fields");
    }

    @Test
    void describe_collectsPriceTypesAndGroupsFromAssortment() {
        when(moyskladClient.getAssortment()).thenReturn(new RemapAssortmentResponse(List.of(
                row("Товары", "Цена продажи"),
                row("Услуги", "Оптовая цена"))));

        CatalogConfigView config = catalogConfigService.describe();

        assertThat(config.priceTypes()).containsExactly("Оптовая цена", "Цена продажи");
        assertThat(config.groups()).containsExactly("Товары", "Услуги");
        assertThat(config.fields()).containsExactly("name", "article", "group", "price", "stock");
        assertThat(config.defaults()).isNotNull();
        assertThat(config.defaults().fields()).containsExactlyElementsOf(CatalogOptions.ALL_FIELDS);
        assertThat(config.defaults().groups()).containsExactly("Товары", "Услуги");
    }

    @Test
    void parsePage_validParameters_returnsPageOptions() {
        PageOptions pageOptions = catalogConfigService.parsePage(2, 50);

        assertThat(pageOptions.page()).isEqualTo(2);
        assertThat(pageOptions.size()).isEqualTo(50);
    }

    @Test
    void parsePage_negativePage_throwsInvalidPaginationException() {
        assertThatThrownBy(() -> catalogConfigService.parsePage(-1, 20))
                .isInstanceOf(InvalidPaginationException.class)
                .hasMessageContaining("page");
    }

    @Test
    void parsePage_sizeOutOfRange_throwsInvalidPaginationException() {
        assertThatThrownBy(() -> catalogConfigService.parsePage(0, 0))
                .isInstanceOf(InvalidPaginationException.class)
                .hasMessageContaining("size");
        assertThatThrownBy(() -> catalogConfigService.parsePage(0, CatalogConfigService.MAX_PAGE_SIZE + 1))
                .isInstanceOf(InvalidPaginationException.class)
                .hasMessageContaining("size");
    }

    private static RemapAssortmentRow row(String group, String priceType) {
        return new RemapAssortmentRow(
                "id-" + group,
                "Product " + group,
                "ART-" + group,
                group,
                List.of(new RemapSalePrice(new BigDecimal("100000"), new RemapPriceType(priceType))));
    }
}
