package com.example.moysklad.api.client;

import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapCustomerOrderDto;
import com.example.moysklad.api.client.dto.RemapCustomerOrderResponseDto;
import com.example.moysklad.api.client.dto.RemapMetaDto;
import com.example.moysklad.api.client.dto.RemapOrderPositionDto;
import com.example.moysklad.api.client.dto.RemapStockCurrentRow;
import com.example.moysklad.api.config.MoyskladProperties;
import com.example.moysklad.api.exception.MoyskladApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link MoyskladClient} chunked assortment fetching.
 */
class MoyskladClientTest {

    private static final String BASE_URL = "https://api.moysklad.ru/api/remap/1.2";
    private static final String ASSORTMENT_URL = BASE_URL + "/entity/assortment";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void getAssortment_singlePartialPage_returnsRowsWithoutSecondRequest() {
        server.expect(requestTo(ASSORTMENT_URL + "?limit=1000&offset=0"))
                .andRespond(withSuccess(rowsJson(row("1")), MediaType.APPLICATION_JSON));

        RemapAssortmentResponse response = client(1000).getAssortment();

        assertThat(response.rows())
                .extracting(RemapAssortmentRow::id)
                .containsExactly("1");
        server.verify();
    }

    @Test
    void getAssortment_severalChunks_concatenatesRowsWithIncreasingOffset() {
        server.expect(requestTo(ASSORTMENT_URL + "?limit=2&offset=0"))
                .andRespond(withSuccess(rowsJson(row("1"), row("2")), MediaType.APPLICATION_JSON));
        server.expect(requestTo(ASSORTMENT_URL + "?limit=2&offset=2"))
                .andRespond(withSuccess(rowsJson(row("3")), MediaType.APPLICATION_JSON));

        RemapAssortmentResponse response = client(2).getAssortment();

        assertThat(response.rows())
                .extracting(RemapAssortmentRow::id)
                .containsExactly("1", "2", "3");
        server.verify();
    }

    @Test
    void getAssortment_exactMultipleOfChunkSize_stopsOnEmptyPage() {
        server.expect(requestTo(ASSORTMENT_URL + "?limit=2&offset=0"))
                .andRespond(withSuccess(rowsJson(row("1"), row("2")), MediaType.APPLICATION_JSON));
        server.expect(requestTo(ASSORTMENT_URL + "?limit=2&offset=2"))
                .andRespond(withSuccess(rowsJson(row("3"), row("4")), MediaType.APPLICATION_JSON));
        server.expect(requestTo(ASSORTMENT_URL + "?limit=2&offset=4"))
                .andRespond(withSuccess(rowsJson(), MediaType.APPLICATION_JSON));

        RemapAssortmentResponse response = client(2).getAssortment();

        assertThat(response.rows())
                .extracting(RemapAssortmentRow::id)
                .containsExactly("1", "2", "3", "4");
        server.verify();
    }

    @Test
    void getAssortment_emptyCatalog_returnsEmptyRows() {
        server.expect(requestTo(ASSORTMENT_URL + "?limit=1000&offset=0"))
                .andRespond(withSuccess(rowsJson(), MediaType.APPLICATION_JSON));

        RemapAssortmentResponse response = client(1000).getAssortment();

        assertThat(response.rows()).isEmpty();
        server.verify();
    }

    @Test
    void getAssortment_nullRowsInResponse_treatedAsEndOfCatalog() {
        server.expect(requestTo(ASSORTMENT_URL + "?limit=1000&offset=0"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RemapAssortmentResponse response = client(1000).getAssortment();

        assertThat(response.rows()).isEmpty();
        server.verify();
    }

    @Test
    void getAssortment_failingChunk_throwsMoyskladApiException() {
        server.expect(requestTo(ASSORTMENT_URL + "?limit=2&offset=0"))
                .andRespond(withSuccess(rowsJson(row("1"), row("2")), MediaType.APPLICATION_JSON));
        server.expect(requestTo(ASSORTMENT_URL + "?limit=2&offset=2"))
                .andRespond(withServerError());

        MoyskladClient client = client(2);

        assertThatThrownBy(client::getAssortment)
                .isInstanceOf(MoyskladApiException.class)
                .hasMessageContaining("500")
                .hasMessageContaining("offset 2");
    }

    @Test
    void getCurrentStocks_requestsCurrentReportWithPhysicalStock() {
        server.expect(requestTo(BASE_URL + "/report/stock/all/current?stockType=stock"))
                .andRespond(withSuccess("""
                        [{"assortmentId":"1","stock":3.0},{"assortmentId":"2","stock":4.5}]
                        """, MediaType.APPLICATION_JSON));

        List<RemapStockCurrentRow> rows = client(1000).getCurrentStocks();

        assertThat(rows)
                .extracting(RemapStockCurrentRow::assortmentId)
                .containsExactly("1", "2");
        assertThat(rows.getFirst().stock()).isEqualByComparingTo("3.0");
        assertThat(rows.getLast().stock()).isEqualByComparingTo("4.5");
        server.verify();
    }

    @Test
    void getCurrentStocks_changedSince_requestsDeltaInMoscowTimeWithoutZeroLines() {
        server.expect(requestTo(BASE_URL
                        + "/report/stock/all/current?stockType=stock"
                        + "&changedSince=2026-11-10%2000:30:00"))
                .andRespond(withSuccess("""
                        [{"assortmentId":"1","stock":0}]
                        """, MediaType.APPLICATION_JSON));

        List<RemapStockCurrentRow> rows =
                client(1000).getCurrentStocks(Instant.parse("2026-11-09T21:30:00Z"));

        assertThat(rows).singleElement()
                .satisfies(row -> assertThat(row.stock()).isEqualByComparingTo("0"));
        server.verify();
    }

    @Test
    void getCurrentStocks_deltaServerError_throwsMoyskladApiException() {
        server.expect(requestTo(BASE_URL
                        + "/report/stock/all/current?stockType=stock"
                        + "&changedSince=2026-11-09%2013:15:00"))
                .andRespond(withServerError());

        MoyskladClient client = client(1000);
        Instant changedSince = Instant.parse("2026-11-09T10:15:00Z");

        assertThatThrownBy(() -> client.getCurrentStocks(changedSince))
                .isInstanceOf(MoyskladApiException.class)
                .hasMessageContaining("500");
    }

    @Test
    void getCurrentStocks_deltaRejectedWith412_includesMoySkladErrorDetails() {
        server.expect(requestTo(BASE_URL
                        + "/report/stock/all/current?stockType=stock"
                        + "&changedSince=2026-11-09%2013:15:00"))
                .andRespond(withStatus(HttpStatus.PRECONDITION_FAILED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"errors":[{"code":1034,"message":"parameter changedSince cannot be used together with include=zeroLines"}]}
                                """));

        MoyskladClient client = client(1000);
        Instant changedSince = Instant.parse("2026-11-09T10:15:00Z");

        assertThatThrownBy(() -> client.getCurrentStocks(changedSince))
                .isInstanceOf(MoyskladApiException.class)
                .hasMessageContaining("412")
                .hasMessageContaining("1034")
                .hasMessageContaining("changedSince");
    }

    @Test
    void createCustomerOrder_postsPayloadAndReturnsCreatedOrder() {
        server.expect(requestTo(BASE_URL + "/entity/customerorder"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"id":"order-1","name":"00001","sum":1000.0}
                        """, MediaType.APPLICATION_JSON));

        RemapCustomerOrderDto payload = new RemapCustomerOrderDto(
                new RemapMetaDto(BASE_URL + "/entity/organization/org-1", "organization", "application/json"),
                new RemapMetaDto(BASE_URL + "/entity/counterparty/agent-1", "counterparty", "application/json"),
                List.of(new RemapOrderPositionDto(
                        new BigDecimal("2"),
                        new BigDecimal("500.00"),
                        new RemapMetaDto(BASE_URL + "/entity/product/p-1", "product", "application/json"))));

        RemapCustomerOrderResponseDto created = client(1000).createCustomerOrder(payload);

        assertThat(created.id()).isEqualTo("order-1");
        assertThat(created.name()).isEqualTo("00001");
        assertThat(created.sum()).isEqualByComparingTo("1000.0");
        server.verify();
    }

    @Test
    void createCustomerOrder_httpError_throwsMoyskladApiExceptionWithDetails() {
        server.expect(requestTo(BASE_URL + "/entity/customerorder"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"errors":[{"code":3004,"message":"bad request"}]}
                                """));

        RemapCustomerOrderDto payload = new RemapCustomerOrderDto(
                new RemapMetaDto(BASE_URL + "/entity/organization/org-1", "organization", "application/json"),
                new RemapMetaDto(BASE_URL + "/entity/counterparty/agent-1", "counterparty", "application/json"),
                List.of());

        assertThatThrownBy(() -> client(1000).createCustomerOrder(payload))
                .isInstanceOf(MoyskladApiException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("3004");
    }

    @Test
    void getCurrentStocks_ignoresFieldsNotRequestedByStockType() {
        server.expect(requestTo(BASE_URL + "/report/stock/all/current?stockType=stock"))
                .andRespond(withSuccess("""
                        [{"assortmentId":"1","stock":3,"reserve":1,"inTransit":2,"quantity":0}]
                        """, MediaType.APPLICATION_JSON));

        List<RemapStockCurrentRow> rows = client(1000).getCurrentStocks();

        assertThat(rows).singleElement()
                .satisfies(row -> assertThat(row.stock()).isEqualByComparingTo("3"));
        server.verify();
    }

    @Test
    void getCurrentStocks_emptyReport_returnsEmptyList() {
        server.expect(requestTo(BASE_URL + "/report/stock/all/current?stockType=stock"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client(1000).getCurrentStocks()).isEmpty();
        server.verify();
    }

    @Test
    void getCurrentStocks_serverError_throwsMoyskladApiException() {
        server.expect(requestTo(BASE_URL + "/report/stock/all/current?stockType=stock"))
                .andRespond(withServerError());

        MoyskladClient client = client(1000);

        assertThatThrownBy(client::getCurrentStocks)
                .isInstanceOf(MoyskladApiException.class)
                .hasMessageContaining("500");
    }

    private MoyskladClient client(int pageSize) {
        return new MoyskladClient(
                new MoyskladProperties("test-token", BASE_URL, pageSize),
                restClientBuilder);
    }

    private static String rowsJson(String... rows) {
        return "{\"rows\":[" + String.join(",", rows) + "]}";
    }

    private static String row(String id) {
        return """
                {"id":"%s","name":"Product %s","article":"ART-%s","pathName":"Товары","salePrices":[]}
                """.formatted(id, id, id).trim();
    }
}
