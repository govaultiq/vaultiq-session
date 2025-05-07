package vaultiq.session.cache.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import vaultiq.session.config.VaultiqSessionProperties;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "vaultiq.session.persistence.cache", name = "enabled", havingValue = "true")
public class VaultiqCacheContext {

    private static final Logger log = LoggerFactory.getLogger(VaultiqCacheContext.class);

    private final Cache sessionPoolCache;
    private final Cache userSessionMappingCache;
    private final Cache blocklistCache;

    public VaultiqCacheContext(
            VaultiqSessionProperties props,
            Map<String, CacheManager> cacheManagers) {
        String configuredCacheManagerName = props.getCache().getManager();
        if (configuredCacheManagerName == null || !cacheManagers.containsKey(configuredCacheManagerName)) {
            log.error("CacheManager `{}` not found, isConfigured: {}", configuredCacheManagerName,
                    configuredCacheManagerName != null && !configuredCacheManagerName.isBlank());
            throw new IllegalStateException("Required CacheManager '" + configuredCacheManagerName + "' not found.");
        }

        CacheManager cacheManager = cacheManagers.get(configuredCacheManagerName);
        VaultiqSessionProperties.CacheNames cacheNames = props.getCache().getCacheNames();

        this.sessionPoolCache = getRequiredCache(cacheManager, cacheNames.getSessions());
        this.userSessionMappingCache = getOptionalCache(cacheManager, cacheNames.getUserSessionMapping(), "user-session-mapping");
        this.blocklistCache = getOptionalCache(cacheManager, cacheNames.getBlocklist(), "blocklist");
    }

    private Cache getRequiredCache(CacheManager cacheManager, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Missing cache name for type 'session-pool'");
        }
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalStateException("Cache named '" + name + "' not found in configured CacheManager.");
        }
        return cache;
    }

    private Cache getOptionalCache(CacheManager manager, String name, String label) {
        if (name == null || name.isBlank()) {
            log.warn("No cache name configured for '{}', falling back to session-pool.", label);
            return sessionPoolCache;
        }
        Cache cache = manager.getCache(name);
        if (cache == null) {
            log.warn("Cache '{}' not found in CacheManager, falling back to session-pool.", name);
            return sessionPoolCache;
        }
        return cache;
    }

    public Cache getSessionPoolCache() {
        return sessionPoolCache;
    }

    public Cache getUserSessionMappingCache() {
        return userSessionMappingCache;
    }

    public Cache getBlocklistCache() {
        return blocklistCache;
    }
}
