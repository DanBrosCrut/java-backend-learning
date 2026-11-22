package com.example.moysklad.api.service;

/**
 * Outcome of an in-memory cache lookup.
 */
public enum CacheStatus {

    /**
     * The value was already in the cache, so no upstream MoySklad call was made.
     */
    HIT,

    /**
     * The value was missing from the cache and had to be loaded from MoySklad.
     */
    MISS;

    /**
     * Combines the outcomes of two cache lookups that both contributed to one response.
     * The result is a {@link #HIT} only when neither lookup reached MoySklad.
     *
     * @param first  the first lookup outcome
     * @param second the second lookup outcome
     * @return {@link #HIT} when both lookups were hits, {@link #MISS} otherwise
     */
    public static CacheStatus combine(CacheStatus first, CacheStatus second) {
        return first == HIT && second == HIT ? HIT : MISS;
    }
}
