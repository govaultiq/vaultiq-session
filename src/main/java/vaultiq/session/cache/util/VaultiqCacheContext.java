package vaultiq.session.cache.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import vaultiq.session.config.VaultiqSessionProperties;

import java.util.Map;

/**
 * Context utility class to provide access to caches configured via Vaultiq session properties.
 * Supports mandatory and optional cache resolution with fallback mechanisms.
 */
@Component
@ConditionalOnProperty(prefix = "vaultiq.session.persistence.cache", name = "enabled", havingValue = "true")
public class VaultiqCacheContext {

    private static final Logger log = LoggerFactory.getLogger(VaultiqCacheContext.class);

    private final CacheManager cacheManager;

    /**
     * Constructs the cache context with a resolved CacheManager.
     *
     * @param props          session properties containing cache manager configuration
     * @param cacheManagers  available cache managers
     */
    public VaultiqCacheContext(
            VaultiqSessionProperties props,
            Map<String, CacheManager> cacheManagers) {
        String configuredCacheManagerName = props.getPersistence().getCacheConfig().getManager();
        if (configuredCacheManagerName == null || !cacheManagers.containsKey(configuredCacheManagerName)) {
            log.error("CacheManager `{}` not found, isConfigured: {}", configuredCacheManagerName,
                    configuredCacheManagerName != null && !configuredCacheManagerName.isBlank());
            throw new IllegalStateException("Required CacheManager '" + configuredCacheManagerName + "' not found.");
        }

        this.cacheManager = cacheManagers.get(configuredCacheManagerName);
    }

    /**
     * Resolves a mandatory cache by name.
     *
     * @param name      the cache name
     * @param property  the associated configuration property for error context
     * @return the resolved cache
     */
    public Cache getCacheMandatory(String name, String property) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Missing cache name for the property: '" + property + "'.");
        }
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalStateException("Cache named '" + name + "' not found in configured CacheManager.");
        }
        return cache;
    }

    /**
     * Starts resolution of an optional cache with potential fallback.
     *
     * @param name      the preferred cache name
     * @param property  the associated property name (used in logging)
     * @return an OptionalCacheResolver to define fallback strategy
     */
    public OptionalCacheResolver getCacheOptional(String name, String property) {
        return new OptionalCacheResolver(name, property, cacheManager);
    }

    /**
     * Helper class for resolving optional caches with support for fallbacks.
     */
    public static class OptionalCacheResolver {
        private final String requiredCacheName;
        private final String requiredProperty;
        private final CacheManager cacheManager;

        public OptionalCacheResolver(String requiredCacheName, String requiredProperty, CacheManager cacheManager) {
            this.requiredCacheName = requiredCacheName;
            this.requiredProperty = requiredProperty;
            this.cacheManager = cacheManager;
        }

        /**
         * Attempts to resolve the configured cache or fallbacks to a provided cache.
         *
         * @param fallbackCache     the fallback Cache object
         * @param fallbackProperty  name of the fallback source property (used in logs)
         * @return resolved Cache
         */
        public Cache orFallbackTo(Cache fallbackCache, String fallbackProperty) {
            var cache = tryGetCache();
            if (cache != null)
                return cache;

            logFallingBack(fallbackProperty);
            if (fallbackCache != null && fallbackProperty != null)
                return fallbackCache;
            else
                throw new IllegalStateException("Failed to fallback, due to fallbackCache = " + (fallbackCache == null) + " and fallbackProperty = " + (fallbackProperty == null));
        }

        /**
         * Attempts to resolve the configured cache or fallbacks to a cache by name.
         *
         * @param fallbackCacheName     fallback cache name
         * @param fallbackProperty      fallback property name (used for error/log context)
         * @return resolved Cache
         */
        public Cache orFallbackTo(String fallbackCacheName, String fallbackProperty) {
            var cache = tryGetCache();
            if (cache != null)
                return cache;

            logFallingBack(fallbackProperty);

            if (fallbackCacheName == null || fallbackCacheName.isBlank()) {
                throw new IllegalStateException("Missing cache name for the property: '" + fallbackProperty + "'.");
            }
            cache = cacheManager.getCache(fallbackCacheName);
            if (cache == null) {
                throw new IllegalStateException("Cache named '" + fallbackCacheName + "' not found in configured CacheManager.");
            }
            return cache;
        }

        private void logFallingBack(String fallbackProperty) {
            log.warn("Cache '{}' not found in CacheManager, falling back to '{}'.", requiredCacheName, fallbackProperty);
        }

        private Cache tryGetCache() {
            if (requiredCacheName == null || requiredCacheName.isBlank()) {
                log.warn("No cache name configured for property: '{}', falling back to session-pool.", requiredProperty);
                return null;
            }

            Cache cache = cacheManager.getCache(requiredCacheName);
            if (cache != null)
                log.debug("Cache '{}' found in CacheManager, returning it.", requiredCacheName);

            return cache;
        }
    }
}