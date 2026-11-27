package com.example.moysklad.api.client.dto;

/**
 * Remap meta reference used to link entities inside order payloads.
 *
 * @param href      the full entity URL in the Remap API
 * @param type      the entity type, e.g. {@code organization}, {@code counterparty} or {@code product}
 * @param mediaType always {@code application/json}
 */
public record RemapMetaDto(
        String href,
        String type,
        String mediaType) {
}
