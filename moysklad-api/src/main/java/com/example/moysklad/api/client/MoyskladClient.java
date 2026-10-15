package com.example.moysklad.api.client;

import com.example.moysklad.api.config.MoyskladProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for the MoySklad Remap 1.2 JSON API.
 */
@Component
public class MoyskladClient {

    private final RestClient restClient;

    /**
     * Creates the client and configures it with the MoySklad base URL and bearer token.
     *
     * @param properties the typed MoySklad configuration properties
     */
    public MoyskladClient(MoyskladProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .build();
    }

    /**
     * Fetches the assortment catalog from the Remap 1.2 API.
     *
     * @return the raw assortment JSON response as a string
     */
    public String getAssortment() {
        return restClient.get()
                .uri("/entity/assortment")
                .retrieve()
                .body(String.class);
    }
}
