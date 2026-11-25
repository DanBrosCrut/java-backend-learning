package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Response DTO for {@code POST /api/orders} — the order created in MoySklad.
 *
 * @param id   the order UUID in MoySklad
 * @param name the order number shown in the MoySklad UI
 * @param sum  the order total in the order currency
 */
@Schema(description = "The customer order created in MoySklad")
public record OrderResponse(
        @Schema(description = "Order UUID in MoySklad", example = "5aec8ab5-2a3d-4b17-9f22-abc123def456")
        String id,

        @Schema(description = "Order number in MoySklad", example = "00001")
        String name,

        @Schema(description = "Order total in the order currency", example = "78012.98")
        BigDecimal sum) {
}
