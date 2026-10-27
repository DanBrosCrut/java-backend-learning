package com.example.moysklad.api.dto;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Optional product fields that a catalog client can request via the {@code fields} query option.
 */
public enum CatalogField {

    NAME("name"),
    ARTICLE("article"),
    GROUP("group"),
    PRICE("price"),
    STOCK("stock");

    private final String queryName;

    CatalogField(String queryName) {
        this.queryName = queryName;
    }

    /**
     * Returns the name of the field as it is used in the {@code fields} query parameter.
     *
     * @return the query name of the field
     */
    @JsonValue
    public String queryName() {
        return queryName;
    }

    /**
     * Resolves a field by its query name, ignoring case.
     *
     * @param queryName the value from the {@code fields} query parameter
     * @return the matching field, or empty when the name is unknown
     */
    public static Optional<CatalogField> fromQueryName(String queryName) {
        return Arrays.stream(values())
                .filter(field -> field.queryName.equalsIgnoreCase(queryName))
                .findFirst();
    }

    /**
     * Returns all supported field names in declaration order.
     *
     * @return the comma-joined list of query names, e.g. {@code name,article,group,price,stock}
     */
    public static String allQueryNames() {
        return Arrays.stream(values())
                .map(CatalogField::queryName)
                .collect(Collectors.joining(","));
    }
}
