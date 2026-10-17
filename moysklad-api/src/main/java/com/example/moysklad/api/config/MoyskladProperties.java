package com.example.moysklad.api.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration properties for the MoySklad integration, bound from the {@code moysklad.*} keys.
 *
 * @param token   the MoySklad bearer token
 * @param baseUrl the MoySklad API base URL
 */
@Validated
@ConfigurationProperties(prefix = "moysklad")
public record MoyskladProperties(
        @NotBlank String token,
        @NotBlank String baseUrl
) {
}
