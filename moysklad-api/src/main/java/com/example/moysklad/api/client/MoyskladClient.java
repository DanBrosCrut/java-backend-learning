package com.example.moysklad.api.client;

import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import com.example.moysklad.api.client.dto.RemapAssortmentRow;
import com.example.moysklad.api.client.dto.RemapStockCurrentRow;
import com.example.moysklad.api.config.MoyskladProperties;
import com.example.moysklad.api.exception.MoyskladApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client for the MoySklad Remap 1.2 JSON API.
 * <p>
 * The assortment is read in pages of at most {@code moysklad.assortment-page-size} rows (1000 by default),
 * because Remap returns no more than 1000 rows per request. A single {@link #getAssortment()}
 * call returns the complete snapshot of the account catalog.
 */
@Component
public class MoyskladClient {

    private static final Logger log = LoggerFactory.getLogger(MoyskladClient.class);

    private static final String ASSORTMENT_PATH = "/entity/assortment";
    private static final String STOCK_CURRENT_PATH = "/report/stock/all/current";
    private static final String STOCK_TYPE_STOCK = "stock";

    private final RestClient restClient;
    private final int assortmentPageSize;

    /**
     * Creates the client and configures it with the MoySklad base URL and bearer token.
     *
     * @param properties        the typed MoySklad configuration properties
     * @param restClientBuilder the Spring-managed RestClient builder
     */
    public MoyskladClient(MoyskladProperties properties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .build();
        this.assortmentPageSize = properties.assortmentPageSize();
    }

    /**
     * Fetches the complete assortment catalog from the Remap 1.2 API.
     * <p>
     * Rows are requested in chunks of {@code assortmentPageSize} using {@code limit} and {@code offset};
     * fetching stops on an empty or partially filled page, which means the end of the catalog has been reached.
     *
     * @return the typed assortment response containing every row of the account catalog
     * @throws MoyskladApiException when any chunk request fails with an HTTP error (4xx or 5xx)
     */
    public RemapAssortmentResponse getAssortment() {
        List<RemapAssortmentRow> rows = new ArrayList<>();
        int offset = 0;

        while (true) {
            List<RemapAssortmentRow> pageRows = fetchAssortmentPage(offset);
            rows.addAll(pageRows);
            if (pageRows.size() < assortmentPageSize) {
                break;
            }
            offset += pageRows.size();
        }

        log.debug("Fetched {} assortment rows in chunks of {}", rows.size(), assortmentPageSize);
        return new RemapAssortmentResponse(List.copyOf(rows));
    }

    /**
     * Fetches the current stock report from the Remap 1.2 API ({@code /report/stock/all/current}).
     * <p>
     * The {@code stockType=stock} parameter makes MoySklad return the physical stock on hand
     * (not the accumulated movements of the extended report), which is the value shown in the catalog.
     *
     * @return one row per product with a non-zero current stock; empty when the account has no stock
     * @throws MoyskladApiException when the request fails with an HTTP error (4xx or 5xx)
     */
    public List<RemapStockCurrentRow> getCurrentStocks() {
        try {
            List<RemapStockCurrentRow> rows = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(STOCK_CURRENT_PATH)
                            .queryParam("stockType", STOCK_TYPE_STOCK)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RemapStockCurrentRow>>() {
                    });
            return Optional.ofNullable(rows).orElse(List.of());
        } catch (HttpStatusCodeException ex) {
            throw new MoyskladApiException(
                    "MoySklad current stock report request failed with status " + ex.getStatusCode().value(),
                    ex);
        }
    }

    private List<RemapAssortmentRow> fetchAssortmentPage(int offset) {
        try {
            RemapAssortmentResponse page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(ASSORTMENT_PATH)
                            .queryParam("limit", assortmentPageSize)
                            .queryParam("offset", offset)
                            .build())
                    .retrieve()
                    .body(RemapAssortmentResponse.class);

            return Optional.ofNullable(page)
                    .map(RemapAssortmentResponse::rows)
                    .map(List::copyOf)
                    .orElse(List.of());
        } catch (HttpStatusCodeException ex) {
            throw new MoyskladApiException(
                    "MoySklad assortment request failed with status " + ex.getStatusCode().value()
                            + " at offset " + offset,
                    ex);
        }
    }
}
