package com.example.moysklad.api.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration properties for the MoySklad integration, bound from the {@code moysklad.*} keys.
 *
 * @param token              the MoySklad bearer token
 * @param baseUrl            the MoySklad API base URL
 * @param assortmentPageSize the number of assortment rows requested per Remap page (1..1000)
 */
@Validated
@ConfigurationProperties(prefix = "moysklad")
public record MoyskladProperties(
        @NotBlank String token,
        @NotBlank String baseUrl,
        @Min(1) @Max(MoyskladProperties.MAX_PAGE_SIZE) @DefaultValue("1000") int assortmentPageSize
) {

    /**
     * Hard limit of the Remap 1.2 API: a single assortment request returns at most 1000 rows.
     */
    public static final int MAX_PAGE_SIZE = 1000;
}
