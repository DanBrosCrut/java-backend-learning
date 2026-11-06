package com.example.moysklad.api.client;

import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.config.MoyskladProperties;
import com.example.moysklad.api.exception.MoyskladApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
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
