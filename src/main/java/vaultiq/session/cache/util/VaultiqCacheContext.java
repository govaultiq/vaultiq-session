
package vaultiq.session.cache.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.cache.service.internal.SessionRevocationCacheService;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistenceRequirement;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;

import java.util.Map;

/**
 * Provides centralized access to configured caches for all Vaultiq session model types.
 * <p>
 * Resolves and validates named caches according to session configuration, simplifying cache management for dependent
 * services such as {@link vaultiq.session.cache.service.internal.VaultiqSessionCacheService} and
 * {@link SessionRevocationCacheService}.
 * </p>
 * <ul>
 *   <li>Ensures caches are present and fail-fast if missing/misconfigured.</li>
 *   <li>Supports fallback strategies for optional caches in multimodel environments.</li>
 *   <li>Backed by the cache manager specified in application properties.</li>
 * </ul>
 */
@Component
@ConditionalOnVaultiqPersistenceRequirement(VaultiqPersistenceMethod.USE_CACHE)
public class VaultiqCacheContext {

    private static final Logger log = LoggerFactory.getLogger(VaultiqCacheContext.class);

    private final CacheManager cacheManager;

    /**
     * Initializes the cache context using settings from the application's session properties.
     * Validates the configured cache manager is available.
     *
     * @param props         Vaultiq session properties specifying cache manager details
     * @param cacheManagers candidate cache managers, usually injected by Spring
     * @throws IllegalStateException if the configured cache manager does not exist
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
     * Returns a mandatory existing cache for the given model type and cache name.
     * Fails if the cache is missing or the name is blank.
     *
     * @param name      configured cache name (must not be blank)
     * @param modelType the associated model type for error messaging
     * @return resolved cache (never null)
     * @throws IllegalStateException if the cache cannot be found
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
     * Starts resolution of an optional cache. Supports fluent fallbacks if the preferred cache is missing.
     *
     * @param name      preferred cache name (may be blank/unset)
     * @param modelType the associated model type for fallback/error messaging
     * @return an {@link OptionalCacheResolver} for fluent fallback selection
     */
    public OptionalCacheResolver getCacheOptional(String name, ModelType modelType) {
        return new OptionalCacheResolver(name, modelType, cacheManager);
    }

    /**
     * Fluent cache resolver for optional/secondary caches, supporting fallback-by-name or to a provided cache object.
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
         * Resolves to the preferred cache if present, otherwise returns the provided fallback cache object.
         *
         * @param fallbackCache    the fallback cache (must not be null if needed)
         * @param fallbackModelType used in log/error output for clarity
         * @return resolved cache
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
         * Resolves to the preferred cache if present, otherwise resolves by fallback cache name.
         *
         * @param fallbackCacheName the fallback cache name
         * @param fallbackModelType used in log/error output for clarity
         * @return resolved cache
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
            Logger log = LoggerFactory.getLogger(OptionalCacheResolver.class);
            log.warn("Cache '{}' not found in CacheManager, falling back to type: '{}'.",
                    requiredCacheName, fallbackModelType.name());
        }

        private Cache tryGetCache() {
            if (requiredCacheName == null || requiredCacheName.isBlank()) {
                Logger log = LoggerFactory.getLogger(OptionalCacheResolver.class);
                log.warn("No cache name configured for property: '{}', will use fallback.", requiredModelType.name());
                return null;
            }
            Cache cache = cacheManager.getCache(requiredCacheName);
            if (cache != null) {
                Logger log = LoggerFactory.getLogger(OptionalCacheResolver.class);
                log.debug("Cache '{}' found in CacheManager, using it.", requiredCacheName);
            }
            return cache;
        }
    }
}
