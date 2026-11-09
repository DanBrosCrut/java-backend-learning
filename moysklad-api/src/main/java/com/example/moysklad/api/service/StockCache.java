package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.config.StockCacheProperties;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory cache of the current stock report with delta synchronization.
 * <p>
 * The cache applies three thresholds:
 * <ul>
 *     <li>younger than {@code moysklad.stock-cache.ttl} (5 minutes by default) — served as is,
 *     without any request to MoySklad;</li>
 *     <li>older than the TTL, but the last full sync is younger than
 *     {@code moysklad.stock-cache.full-sync-interval} (24 hours by default) — only rows changed since
 *     the last successful update are requested with {@code changedSince} and merged into the snapshot;</li>
 *     <li>no full sync yet, or the last one is at least the full sync interval old — the complete report
 *     replaces the snapshot.</li>
 * </ul>
 * A forced refresh always performs a full report, bypassing both time thresholds.
 * Failed loads leave the previous snapshot untouched, so the next lookup retries instead of caching an error.
 */
@Component
public class StockCache {

    private static final Logger log = LoggerFactory.getLogger(StockCache.class);

    private final MoyskladClient moyskladClient;
    private final StockReportMapper stockReportMapper;
    private final StockCacheProperties properties;
    private final Clock clock;
    private final Object lock = new Object();

    private Snapshot snapshot;

    /**
     * Creates the cache backed by the MoySklad client, the stock report mapper and the cache thresholds.
     *
     * @param moyskladClient    the client used to load the full or delta stock report
     * @param stockReportMapper the mapper turning report rows into a lookup map
     * @param properties        the TTL and full sync interval
     * @param clock             the clock used to evaluate the thresholds
     */
    public StockCache(MoyskladClient moyskladClient,
                      StockReportMapper stockReportMapper,
                      StockCacheProperties properties,
                      Clock clock) {
        this.moyskladClient = moyskladClient;
        this.stockReportMapper = stockReportMapper;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Returns the cached current stock map, refreshing it from MoySklad when a threshold is crossed.
     * <p>
     * A lookup within the TTL reports {@link CacheStatus#HIT} and makes no request. A delta or full refresh
     * reports {@link CacheStatus#MISS}. When {@code forceRefresh} is {@code true}, the TTL and delta mode are
     * skipped and the complete report always replaces the snapshot.
     *
     * @param forceRefresh whether to force a full refresh regardless of the time thresholds
     * @return the product ID to stock quantity map plus the cache status of this lookup
     */
    public CachedResult<Map<String, BigDecimal>> get(boolean forceRefresh) {
        synchronized (lock) {
            Instant now = clock.instant();
            if (!forceRefresh && isFresh(now)) {
                log.info("Current stock cache HIT: {} product(s)", snapshot.stocks().size());
                return new CachedResult<>(snapshot.stocks(), CacheStatus.HIT);
            }

            snapshot = forceRefresh || shouldFullSync(now) ? loadFull() : loadDelta();
            return new CachedResult<>(snapshot.stocks(), CacheStatus.MISS);
        }
    }

    /**
     * Drops the cached snapshot, so the next lookup reloads the complete stock report from MoySklad.
     */
    public void invalidate() {
        synchronized (lock) {
            snapshot = null;
        }
        log.info("Current stock cache invalidated: the next request reloads the full stock report from MoySklad");
    }

    private boolean isFresh(Instant now) {
        return snapshot != null && now.isBefore(snapshot.lastUpdate().plus(properties.ttl()));
    }

    private boolean shouldFullSync(Instant now) {
        return snapshot == null
                || !now.isBefore(snapshot.lastFullSync().plus(properties.fullSyncInterval()));
    }

    private Snapshot loadFull() {
        Map<String, BigDecimal> stocks = stockReportMapper.toStockMap(moyskladClient.getCurrentStocks());
        Instant loadedAt = clock.instant();
        log.info("Current stock cache MISS (full): {} product(s)", stocks.size());
        return new Snapshot(stocks, loadedAt, loadedAt);
    }

    private Snapshot loadDelta() {
        Instant changedSince = snapshot.lastUpdate();
        Map<String, BigDecimal> changes =
                stockReportMapper.toStockMap(moyskladClient.getCurrentStocks(changedSince));
        Map<String, BigDecimal> merged = new HashMap<>(snapshot.stocks());
        merged.putAll(changes);
        Map<String, BigDecimal> stocks = Map.copyOf(merged);
        Instant loadedAt = clock.instant();
        log.info("Current stock cache MISS (delta since {}): {} changed product(s), {} product(s) in total",
                changedSince, changes.size(), stocks.size());
        return new Snapshot(stocks, snapshot.lastFullSync(), loadedAt);
    }

    private record Snapshot(Map<String, BigDecimal> stocks, Instant lastFullSync, Instant lastUpdate) {
    }
}
