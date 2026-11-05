package com.example.moysklad.api.service;

/**
 * A value returned together with the cache status of the lookup that produced it.
 *
 * @param body   the value
 * @param status whether the value came from the cache
 * @param <T>    the value type
 */
public record CachedResult<T>(T body, CacheStatus status) {
}
