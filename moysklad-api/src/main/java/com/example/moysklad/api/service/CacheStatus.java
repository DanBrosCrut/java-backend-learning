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
    MISS
}
