package com.example.health_service.client;

import org.springframework.beans.factory.annotation.Value;
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
     * @param baseUrl the MoySklad API base URL
     * @param token   the MoySklad bearer token
     */
    public MoyskladClient(
            @Value("${moysklad.base-url}") String baseUrl,
            @Value("${moysklad.token}") String token) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
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
