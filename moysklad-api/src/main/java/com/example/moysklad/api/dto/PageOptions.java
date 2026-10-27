package com.example.moysklad.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Validated pagination parameters of the catalog request.
 *
 * @param page the zero-based page index
 * @param size the maximum number of items per page
 */
@Schema(description = "Pagination parameters of the catalog request")
public record PageOptions(
        @Schema(description = "Zero-based page index", example = "0")
        int page,
        @Schema(description = "Maximum number of items per page", example = "20")
        int size) {
}
