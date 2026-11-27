package com.example.moysklad.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed configuration properties for customer order creation, bound from the
 * {@code moysklad.orders.*} keys.
 *
 * @param organizationId the MoySklad organization used as the seller of the order
 * @param agentId        the MoySklad counterparty (agent) used as the buyer of the order
 */
@ConfigurationProperties(prefix = "moysklad.orders")
public record OrderProperties(
        @DefaultValue("") String organizationId,
        @DefaultValue("") String agentId) {
}
