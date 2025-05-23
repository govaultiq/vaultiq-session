package vaultiq.session.cache.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.util.CacheType;
import vaultiq.session.cache.util.VaultiqCacheContext;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.context.VaultiqSessionContext;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.core.model.VaultiqSession;

import java.util.Set;

/**
 * Service for managing session fingerprints in the cache.
 * This service is active only when the session model is configured to use caching.
 */
@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.SESSION)
public class SessionFingerprintCacheService {
    private static final Logger log = LoggerFactory.getLogger(SessionFingerprintCacheService.class);

    private final Cache sessionFingerprintsCache;

    /**
     * Constructs a new SessionFingerprintCacheService.
     * Initializes the cache specifically for session fingerprints.
     *
     * @param cacheContext The context for accessing configured caches.
     * @param sessionContext The session context for retrieving model configurations.
     */
    public SessionFingerprintCacheService(
            VaultiqCacheContext cacheContext,
            VaultiqSessionContext sessionContext
    ) {
        this.sessionFingerprintsCache = cacheContext.getCacheMandatory(
                sessionContext.getModelConfig(CacheType.SESSION_FINGERPRINTS).cacheName(),
                CacheType.SESSION_FINGERPRINTS);
    }

    /**
     * Caches a session's device fingerprint using its session ID as the key.
     *
     * @param session The {@link VaultiqSession} containing the session ID and device fingerprint.
     */
    public void cacheSessionFingerPrint(VaultiqSession session) {
        log.debug("Caching session fingerprint for sessionId={}", session.getSessionId());
        sessionFingerprintsCache.put(session.getSessionId(), session.getDeviceFingerPrint());
    }

    /**
     * Retrieves a session's device fingerprint from the cache.
     *
     * @param sessionId The ID of the session.
     * @return The device fingerprint string, or {@code null} if not found.
     */
    public String getSessionFingerPrint(String sessionId) {
        log.debug("Retrieving session fingerprint for sessionId={}", sessionId);
        return sessionFingerprintsCache.get(sessionId, String.class);
    }

    /**
     * Evicts (removes) a session's device fingerprint from the cache.
     *
     * @param sessionId The ID of the session whose fingerprint is to be evicted.
     */
    public void evictSessionFingerPrint(String sessionId) {
        log.debug("Evicting session fingerprint for sessionId={}", sessionId);
        sessionFingerprintsCache.evict(sessionId);
    }

    public void evictAllSessions(Set<String> sessionIds) {
        log.debug("Evicting all session fingerprints for sessionIds= {}", sessionIds);
        var resultList = sessionIds.stream().map(sessionFingerprintsCache::evictIfPresent).toList();
        log.info("{} of {} session fingerprints found and evicted.", resultList.size(), sessionIds.size());
    }
}