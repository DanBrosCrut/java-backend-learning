package com.example.moysklad.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration of the shared {@link RestClient.Builder}.
 * <p>
 * Spring Boot no longer auto-registers a {@code RestClient.Builder} bean in this setup,
 * so the application declares it explicitly and lets each client derive its own immutable
 * {@link RestClient} from it. The builder is also what {@code MockRestServiceServer} binds to in tests.
 */
@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    /**
     * Creates the {@link RestClient.Builder} bean used by the MoySklad client.
     *
     * @return a fresh RestClient builder
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
