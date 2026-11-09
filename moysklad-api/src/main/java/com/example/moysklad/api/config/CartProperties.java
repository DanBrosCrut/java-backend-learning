package com.example.moysklad.api.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration properties for the file-based cart, bound from the {@code app.cart.*} keys.
 *
 * @param file the path to the cart JSON file, relative to the application working directory
 */
@Validated
@ConfigurationProperties(prefix = "app.cart")
public record CartProperties(
        @NotBlank @DefaultValue("data/cart.json") String file) {
}
