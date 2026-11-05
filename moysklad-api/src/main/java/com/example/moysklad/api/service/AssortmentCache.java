package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.client.dto.RemapAssortmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory cache of the complete assortment snapshot.
 * <p>
 * A full catalog may require many paged requests to MoySklad, so the snapshot is loaded once and then
 * served from a {@link ConcurrentHashMap}. Every lookup reports whether it was a {@link CacheStatus#HIT}
 * or a {@link CacheStatus#MISS}; the caller exposes that value as the {@code X-Cache} response header.
 * A request may force a reload with {@code invalidate = true}.
 */
@Component
public class AssortmentCache {

    private static final Logger log = LoggerFactory.getLogger(AssortmentCache.class);

    private static final String SNAPSHOT_KEY = "assortment";

    private final MoyskladClient moyskladClient;
    private final ConcurrentMap<String, RemapAssortmentResponse> snapshots = new ConcurrentHashMap<>();

    /**
     * Creates the cache backed by the MoySklad client.
     *
     * @param moyskladClient the client used to load the assortment on a cache miss
     */
    public AssortmentCache(MoyskladClient moyskladClient) {
        this.moyskladClient = moyskladClient;
    }

    /**
     * Returns the cached assortment snapshot, loading it from MoySklad on a miss.
     * <p>
     * When {@code invalidate} is {@code true} the current snapshot is dropped first, so the call always
     * reaches MoySklad and reports a {@link CacheStatus#MISS}. A failed load leaves the cache empty,
     * so the next call retries instead of caching an error.
     *
     * @param invalidate whether to drop the current snapshot before reading
     * @return the snapshot plus the cache status of this lookup
     */
    public CachedResult<RemapAssortmentResponse> get(boolean invalidate) {
        if (invalidate) {
            invalidate();
        }

        AtomicBoolean loaded = new AtomicBoolean(false);
        RemapAssortmentResponse snapshot = snapshots.computeIfAbsent(SNAPSHOT_KEY, key -> {
            loaded.set(true);
            return moyskladClient.getAssortment();
        });

        CacheStatus status = loaded.get() ? CacheStatus.MISS : CacheStatus.HIT;
        log.info("Assortment cache {}: {} row(s)", status, rowCount(snapshot));
        return new CachedResult<>(snapshot, status);
    }

    /**
     * Drops the cached assortment snapshot, so the next lookup reloads it from MoySklad.
     */
    public void invalidate() {
        snapshots.clear();
        log.info("Assortment cache invalidated: the next request reloads the catalog from MoySklad");
    }

    private static int rowCount(RemapAssortmentResponse snapshot) {
        return Optional.ofNullable(snapshot.rows()).map(List::size).orElse(0);
    }
}
