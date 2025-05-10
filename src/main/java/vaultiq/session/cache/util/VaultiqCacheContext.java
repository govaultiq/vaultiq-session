
package vaultiq.session.cache.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.VaultiqSessionProperties;

import java.util.Map;
import java.util.Objects;

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
     * @param props session properties containing cache manager configuration
     * @param cacheManagers available cache managers
     */
    public VaultiqCacheContext(VaultiqSessionProperties props, Map<String, CacheManager> cacheManagers) {
        String configuredCacheManagerName = props.getPersistence().getCacheConfig().getManager();
        if (configuredCacheManagerName == null || !cacheManagers.containsKey(configuredCacheManagerName)) {
            log.error("CacheManager '{}' not found. Provided: {}", configuredCacheManagerName,
                    cacheManagers.keySet());
            throw new IllegalStateException("Required CacheManager '" + configuredCacheManagerName + "' not found.");
        }
        this.cacheManager = cacheManagers.get(configuredCacheManagerName);
    }

    /**
     * Resolves a mandatory cache by name.
     *
     * @param name the cache name
     * @param modelType the model type for which the cache is required
     * @return the resolved cache (never null)
     */
    public Cache getCacheMandatory(String name, ModelType modelType) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Missing cache name for the type: '" + modelType.name() + "'.");
        }
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalStateException("Cache named '" + name + "' not found in configured CacheManager.");
        }
        return cache;
    }

    /**
     * Begins optional cache resolution with potential fallback strategies.
     *
     * @param name the preferred cache name (may be null/blank, triggers fallback)
     * @param modelType the model type for which the cache is required
     * @return an OptionalCacheResolver to define fallback strategy
     */
    public OptionalCacheResolver getCacheOptional(String name, ModelType modelType) {
        return new OptionalCacheResolver(name, modelType, cacheManager);
    }

    /**
     * Helper class for resolving optional caches with support for fallbacks.
     */
    public static class OptionalCacheResolver {
        private final String requiredCacheName;
        private final ModelType requiredModelType;
        private final CacheManager cacheManager;

        public OptionalCacheResolver(String requiredCacheName, ModelType requiredModelType, CacheManager cacheManager) {
            this.requiredCacheName = requiredCacheName;
            this.requiredModelType = requiredModelType;
            this.cacheManager = cacheManager;
        }

        /**
         * Attempts to resolve the configured cache or falls back to a provided cache instance.
         *
         * @param fallbackCache the fallback Cache object
         * @param fallbackModelType provides log/clarity for which fallback is used
         * @return resolved Cache
         */
        public Cache orFallbackTo(Cache fallbackCache, ModelType fallbackModelType) {
            Cache cache = tryGetCache();
            if (cache != null)
                return cache;

            logFallingBack(fallbackModelType);

            if (fallbackCache != null)
                return fallbackCache;

            throw new IllegalStateException("Fallback to provided Cache failed: fallbackCache was null.");
        }

        /**
         * Attempts to resolve the configured cache or falls back to a cache by name.
         *
         * @param fallbackCacheName fallback cache name (must be non-null/non-blank)
         * @param fallbackModelType provides log/clarity for which fallback is used
         * @return resolved Cache
         */
        public Cache orFallbackTo(String fallbackCacheName, ModelType fallbackModelType) {
            Cache cache = tryGetCache();
            if (cache != null)
                return cache;

            logFallingBack(fallbackModelType);

            if (fallbackCacheName == null || fallbackCacheName.isBlank()) {
                throw new IllegalStateException("Missing fallback cache name for the property: '" + fallbackModelType + "'.");
            }
            cache = cacheManager.getCache(fallbackCacheName);
            if (cache == null) {
                throw new IllegalStateException("Fallback cache named '" + fallbackCacheName + "' not found in configured CacheManager.");
            }
            return cache;
        }

        private void logFallingBack(ModelType fallbackModelType) {
            log.warn("Cache '{}' not found in CacheManager, falling back to type: '{}'.",
                    requiredCacheName, fallbackModelType.name());
        }

        private Cache tryGetCache() {
            if (requiredCacheName == null || requiredCacheName.isBlank()) {
                log.warn("No cache name configured for property: '{}', will use fallback.", requiredModelType.name());
                return null;
            }
            Cache cache = cacheManager.getCache(requiredCacheName);
            if (cache != null) {
                log.debug("Cache '{}' found in CacheManager, using it.", requiredCacheName);
            }
            return cache;
        }
    }
}
