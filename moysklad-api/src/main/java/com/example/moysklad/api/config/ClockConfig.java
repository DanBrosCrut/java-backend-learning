package com.example.moysklad.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuration of the application clock used by time-aware caches.
 * <p>
 * Tests replace this bean with a fixed or mutable clock to verify time-based thresholds deterministically.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /**
     * Creates the system clock in UTC. All cache timestamps are stored as {@link java.time.Instant},
     * so the system zone does not affect threshold calculations.
     *
     * @return the application clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
