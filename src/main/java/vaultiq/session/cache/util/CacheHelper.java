package vaultiq.session.cache.util;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * A streamlined utility class for performing cache operations (get, put, evict)
 * conditionally based on the existence of a specific cache managed by Spring's CacheManager.
 * This helper is designed to be instantiated for a single, predefined logical cache
 * identified by a {@link CacheType} (typically an Enum).
 * </p>
 * <p>
 * The cache instance is retrieved once during the construction of this helper.
 * If the target cache, identified by its alias, is not registered in the CacheManager
 * at the time of this helper's creation, or if the CacheManager itself is not available,
 * all subsequent operations (get, put, evict) will silently skip without throwing exceptions.
 * This fail-silent approach allows applications to function gracefully even when caching
 * infrastructure is not available or properly configured.
 * </p>
 * <p>
 * When a cache operation is skipped due to missing cache infrastructure, appropriate
 * warning and debug logs are generated to aid in troubleshooting, but the application
 * flow is not disrupted. For get operations, null values are returned; for collection
 * operations, empty collections are returned; and for eviction operations, false is returned.
 * </p>
 * <p>
 * This class is intended to be used as a Spring bean, typically configured in a
 * {@code @Configuration} class of the consuming application and then autowired
 * with {@code @Qualifier} when multiple instances are needed for different {@code CacheType}s.
 * </p>
 */
public class CacheHelper {

    private static final Logger logger = LoggerFactory.getLogger(CacheHelper.class);

    private final Cache cache; // The specific cache instance this helper manages
    private final String cacheName; // The alias of the cache this helper manages

    /**
     * Constructs a new CacheHelper instance.
     * The specific {@link Cache} instance is retrieved from the {@link CacheManager}
     * during initialization based on the provided {@code cacheType}.
     *
     * @param optionalCacheManager The Spring {@code Optional<CacheManager>} instance, which may be empty
     *                            if no CacheManager is available in the application context.
     * @param cacheType    The specific {@link CacheType} instance that defines the alias of the cache this helper will manage.
     *                     (e.g., an enum representing various application caches).
     */
    public CacheHelper(Optional<CacheManager> optionalCacheManager, CacheType cacheType) {
        this.cacheName = cacheType.alias(); // Store cacheName for logging consistency
        this.cache = optionalCacheManager
                .map(cacheManager -> cacheManager.getCache(cacheName))
                .orElse(null);

        if (this.cache == null) {
            logger.warn("CacheHelper initialized; But cache not found for type: '{}'; Skipping all cache operations silently.", cacheName);
        } else {
            logger.info("CacheHelper initialized successfully for cache type: '{}'.", cacheName);
        }
    }

    /**
     * Provides an {@link Optional} wrapper around the managed {@link Cache} instance.
     * This method is used internally to ensure operations only proceed if the cache
     * was successfully retrieved during the helper's construction.
     *
     * @return An {@link Optional} containing the {@link Cache} instance if it was successfully initialized,
     * otherwise an empty Optional if the cache was not found at startup.
     */
    private Optional<Cache> getCache() {
        if (cache == null) {
            logger.debug("Cache '{}' is null. Skipping operation.", cacheName);
        }
        return Optional.ofNullable(cache);
    }

    /**
     * Caches a given data object under a specified key.
     * The operation is performed only if the cache managed by this helper exists.
     * If the cache does not exist (was not found during helper initialization),
     * the operation is silently skipped.
     *
     * @param key  The key under which the data should be stored in the cache. Must not be null.
     * @param data The data object to be cached. Can be null.
     * @param <D>  The type of the data object.
     */
    public <D> void cache(String key, D data) {
        getCache().ifPresent(c -> {
            c.put(key, data);
            logger.debug("Successfully put key '{}' into cache '{}'.", key, cacheName);
        });
    }

    /**
     * Retrieves a cached value for a given key and attempts to cast it to the specified class.
     * The operation is performed only if the cache managed by this helper exists.
     * If the cache does not exist, the key is not found, or the value cannot be cast,
     * {@code null} is returned silently.
     *
     * @param key   The key of the entry to retrieve. Must not be null.
     * @param clazz The expected class type of the cached value.
     * @param <D>   The generic type of the value.
     * @return The cached value, or {@code null} if the cache is not present, the key is not found, or the value type is incompatible.
     */
    public <D> D get(String key, Class<D> clazz) {
        return getCache()
                .map(c -> {
                    D value = c.get(key, clazz);
                    if (value != null) {
                        logger.debug("Successfully retrieved key '{}' from cache '{}'.", key, cacheName);
                    } else {
                        logger.debug("Key '{}' not found in cache '{}'.", key, cacheName);
                    }
                    return value;
                })
                .orElse(null);
    }

    /**
     * Retrieves multiple cached values for a given set of keys, returning them as a {@link Map}.
     * Each key-value pair is included in the map only if the value is found in the cache.
     * The operation is performed only if the cache managed by this helper exists.
     * If the cache does not exist, an empty map is returned silently.
     *
     * @param keys  A {@link Set} of keys to retrieve. Must not be null.
     * @param clazz The expected class type of the cached values.
     * @param <D>   The generic type of the values.
     * @return A {@link Map} where keys are the requested keys and values are the retrieved data.
     * Returns an empty map if the cache is not present or no values are found for the given keys.
     */
    public <D> Map<String, D> getAllAsMap(Set<String> keys, Class<D> clazz) {
        Optional<Cache> cacheOptional = getCache();
        if (cacheOptional.isPresent()) {
            Cache c = cacheOptional.get();
            Map<String, D> results = new HashMap<>();
            logger.debug("Performing getAllAsMap operation for cache '{}' with {} keys.", cacheName, keys.size());
            keys.forEach(key -> {
                D value = c.get(key, clazz);
                if (value != null) {
                    results.put(key, value);
                    logger.trace("Key '{}' found for getAllAsMap in cache '{}'.", key, cacheName);
                } else {
                    logger.trace("Key '{}' not found for getAllAsMap in cache '{}'.", key, cacheName);
                }
            });
            logger.debug("Completed getAllAsMap for cache '{}'. Found {} out of {} keys.", cacheName, results.size(), keys.size());
            return results;
        } else {
            // Logging for cache absence already handled in getCache()
            return Collections.emptyMap(); // Return empty map if cache doesn't exist
        }
    }

    /**
     * Retrieves multiple cached values for a given set of keys, returning them as a {@link Set} of data objects.
     * Only non-null values found in the cache are included in the resulting set.
     * The operation is performed only if the cache managed by this helper exists.
     * If the cache does not exist, an empty set is returned silently.
     *
     * @param keys  A {@link Set} of keys to retrieve. Must not be null.
     * @param clazz The expected class type of the cached values.
     * @param <D>   The generic type of the values.
     * @return A {@link Set} of retrieved data objects. Returns an empty set if the cache is not present or no values are found.
     */
    public <D> Set<D> getAll(Set<String> keys, Class<D> clazz) {
        Optional<Cache> cacheOptional = getCache();
        if (cacheOptional.isPresent()) {
            logger.debug("Performing getAll operation for cache '{}' with {} keys.", cacheName, keys.size());
            return keys.stream()
                    .map(key -> get(key, clazz)) // Reuses the get method, which handles individual key logging
                    .filter(java.util.Objects::nonNull) // Filter out nulls
                    .collect(Collectors.toSet());
        } else {
            // Logging for cache absence already handled in getCache()
            return Collections.emptySet(); // Return empty set if cache doesn't exist
        }
    }

    /**
     * Evicts a specific entry from the cache.
     * The operation is performed only if the cache managed by this helper exists.
     * If the cache does not exist, the operation is silently skipped.
     *
     * @param key The key of the entry to evict. Must not be null.
     * @return {@code true} if the entry was found and evicted, {@code false} if the entry was not found or if the cache itself was not present.
     */
    public boolean evict(String key) {
        return getCache().map(c -> {
            var result = c.evictIfPresent(key);
            logger.debug("Successfully attempted to evict key '{}' from cache '{}'. Result: {}", key, cacheName, result);
            return result;
        }).orElse(false);
    }

    /**
     * Evicts multiple entries from the cache based on a set of keys.
     * Each eviction operation is performed only if the cache managed by this helper exists.
     * If the cache does not exist, all eviction operations are silently skipped.
     *
     * @param keys A {@link Set} of keys to evict. Must not be null.
     */
    public void evictAll(Set<String> keys) {
        Optional<Cache> cacheOptional = getCache();
        if (cacheOptional.isPresent()) {
            logger.debug("Attempting to evict {} entries from cache '{}'", keys.size(), cacheName);
            var evictedCount = keys.stream()
                    .map(this::evict)
                    .filter(Boolean.TRUE::equals)
                    .count();
            logger.debug("{} entries found and evicted from cache '{}'.", evictedCount, cacheName);
        }
    }

    /**
     * <p>
     * This class defines {@code public static final String} constants for the bean names
     * of various {@link CacheHelper} instances within the application context.
     * </p>
     *
     * <p>
     * By centralizing bean names here, it ensures consistency and avoids "magic strings"
     * spread throughout configuration classes. These constants can be directly used
     * in annotations like {@code @Bean(name = ...)} or {@code @Qualifier(...)}
     * because their values are known at compile time.
     * </p>
     */
    public static class BeanNames {
        /**
         * The Spring bean name for the {@code CacheHelper} managing
         * {@link CacheType#SESSION_FINGERPRINTS}.
         * Value: {@value}
         */
        public static final String SESSION_FINGERPRINT_CACHE_HELPER = "sessionFingerprintCacheHelper";

        /**
         * The Spring bean name for the {@code CacheHelper} managing
         * {@link CacheType#SESSION_POOL}.
         * Value: {@value}
         */
        public static final String SESSION_POOL_CACHE_HELPER = "sessionPoolCacheHelper";

        /**
         * The Spring bean name for the {@code CacheHelper} managing
         * {@link CacheType#REVOKED_SIDS}.
         * Value: {@value}
         */
        public static final String REVOKED_SIDS_CACHE_HELPER = "revokedSidsCacheHelper";

        /**
         * The Spring bean name for the {@code CacheHelper} managing
         * {@link CacheType#REVOKED_SESSION_POOL}.
         * Value: {@value}
         */
        public static final String REVOKED_SESSION_POOL_CACHE_HELPER = "revokedSessionPoolCacheHelper";
    }
}