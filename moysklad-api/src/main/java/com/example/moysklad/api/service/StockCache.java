package com.example.moysklad.api.service;

import com.example.moysklad.api.client.MoyskladClient;
import com.example.moysklad.api.mapper.StockReportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory cache of the current stock report.
 * <p>
 * The report is a single cheap request, but the catalog and the availability check both need it,
 * so the parsed product ID to stock map is loaded once and then served from a {@link ConcurrentHashMap}.
 * Every lookup reports {@link CacheStatus#HIT} or {@link CacheStatus#MISS} in the log; the snapshot can be
 * dropped with {@code invalidate = true}.
 */
@Component
public class StockCache {

    private static final Logger log = LoggerFactory.getLogger(StockCache.class);

    private static final String SNAPSHOT_KEY = "stock";

    private final MoyskladClient moyskladClient;
    private final StockReportMapper stockReportMapper;
    private final ConcurrentMap<String, Map<String, BigDecimal>> snapshots = new ConcurrentHashMap<>();

    /**
     * Creates the cache backed by the MoySklad client and the stock report mapper.
     *
     * @param moyskladClient    the client used to load the current stock report on a cache miss
     * @param stockReportMapper the mapper turning report rows into a lookup map
     */
    public StockCache(MoyskladClient moyskladClient, StockReportMapper stockReportMapper) {
        this.moyskladClient = moyskladClient;
        this.stockReportMapper = stockReportMapper;
    }

    /**
     * Returns the cached current stock map, loading it from MoySklad on a miss.
     * <p>
     * When {@code invalidate} is {@code true} the current snapshot is dropped first, so the call always
     * reaches MoySklad and reports a {@link CacheStatus#MISS}. A failed load leaves the cache empty,
     * so the next call retries instead of caching an error.
     *
     * @param invalidate whether to drop the current snapshot before reading
     * @return the product ID to stock quantity map plus the cache status of this lookup
     */
    public CachedResult<Map<String, BigDecimal>> get(boolean invalidate) {
        if (invalidate) {
            invalidate();
        }

        AtomicBoolean loaded = new AtomicBoolean(false);
        Map<String, BigDecimal> stocks = snapshots.computeIfAbsent(SNAPSHOT_KEY, key -> {
            loaded.set(true);
            return stockReportMapper.toStockMap(moyskladClient.getCurrentStocks());
        });

        CacheStatus status = loaded.get() ? CacheStatus.MISS : CacheStatus.HIT;
        log.info("Current stock cache {}: {} product(s)", status, stocks.size());
        return new CachedResult<>(stocks, status);
    }

    /**
     * Drops the cached current stock snapshot, so the next lookup reloads it from MoySklad.
     */
    public void invalidate() {
        snapshots.clear();
        log.info("Current stock cache invalidated: the next request reloads the stock report from MoySklad");
    }
}
