package com.example.moysklad.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Typed configuration properties of the stock cache, bound from the {@code moysklad.stock-cache.*} keys.
 *
 * @param ttl              how long a stock snapshot is served without any request to MoySklad
 * @param fullSyncInterval how long the last full stock report stays authoritative; after it the cache
 *                         always reloads the complete report
 */
@ConfigurationProperties(prefix = "moysklad.stock-cache")
public record StockCacheProperties(
        @DefaultValue("PT5M") Duration ttl,
        @DefaultValue("PT24H") Duration fullSyncInterval
) {

    /**
     * Validates that both thresholds are positive durations.
     *
     * @throws IllegalArgumentException when a threshold is missing, zero or negative
     */
    public StockCacheProperties {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("moysklad.stock-cache.ttl must be a positive duration");
        }
        if (fullSyncInterval == null || fullSyncInterval.isZero() || fullSyncInterval.isNegative()) {
            throw new IllegalArgumentException(
                    "moysklad.stock-cache.full-sync-interval must be a positive duration");
        }
    }

    /**
     * Returns the defaults used when the properties are not overridden:
     * a 5-minute TTL and a 24-hour full sync interval.
     *
     * @return the default stock cache properties
     */
    public static StockCacheProperties defaults() {
        return new StockCacheProperties(Duration.ofMinutes(5), Duration.ofHours(24));
    }
}
