package com.example.moysklad.api.dto;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Direction of the catalog sorting requested via the {@code order} query option.
 */
public enum SortOrder {

    /**
     * Ascending order (A → Z, cheapest → most expensive).
     */
    ASC,

    /**
     * Descending order (Z → A, most expensive → cheapest).
     */
    DESC;

    /**
     * Resolves a sort order by its query value, ignoring case.
     *
     * @param value the value from the {@code order} query parameter
     * @return the matching order, or empty when the value is unknown
     */
    public static Optional<SortOrder> fromValue(String value) {
        return Arrays.stream(values())
                .filter(order -> order.name().equalsIgnoreCase(value))
                .findFirst();
    }

    /**
     * Returns the lower-case value used in the {@code order} query parameter.
     *
     * @return {@code asc} or {@code desc}
     */
    public String queryName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
