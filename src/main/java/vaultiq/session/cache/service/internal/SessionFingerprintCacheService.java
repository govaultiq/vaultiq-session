package vaultiq.session.cache.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.util.CacheHelper;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.domain.model.ClientSession;
import vaultiq.session.domain.model.ModelType;

import java.util.Objects; // Added for Objects.requireNonNull
import java.util.Set;

/**
 * <p>Service for managing session fingerprints within the application's cache layer.</p>
 *
 * <p>This service provides dedicated functionality for storing, retrieving, and
 * evicting device fingerprints associated with user sessions. It acts as an
 * in-memory cache for quick lookups of session fingerprint data.</p>
 *
 * <p>The service is conditionally enabled: it will only be instantiated by Spring
 * if the application's session model is configured to use caching (i.e.,
 * {@link VaultiqPersistenceMethod#USE_CACHE} for {@link ModelType#SESSION}).</p>
 *
 * @see CacheHelper
 * @see ClientSession
 * @see ConditionalOnVaultiqModelConfig
 */
@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.SESSION)
public class SessionFingerprintCacheService {
    private static final Logger log = LoggerFactory.getLogger(SessionFingerprintCacheService.class);

    private final CacheHelper sessionFingerprintsCache;

    /**
     * Constructs the {@code SessionFingerprintCacheService}.
     * Spring automatically injects the {@link CacheHelper} bean qualified for session fingerprints.
     *
     * @param sessionFingerprintsCache The {@link CacheHelper} instance specifically configured
     * for managing session fingerprint data in the cache. Cannot be null.
     */
    public SessionFingerprintCacheService(
            @Qualifier(CacheHelper.BeanNames.SESSION_FINGERPRINT_CACHE_HELPER)
            CacheHelper sessionFingerprintsCache
    ) {
        this.sessionFingerprintsCache = Objects.requireNonNull(sessionFingerprintsCache,
                "SessionFingerprintsCacheHelper bean not found or is null.");
    }

    /**
     * Caches a session's device fingerprint, using the session ID as the unique key.
     * If a fingerprint for the given session ID already exists, it will be overwritten.
     *
     * @param session The {@link ClientSession} object which must contain a non-null
     * session ID and device fingerprint.
     * @throws NullPointerException if the provided {@code session} is {@code null},
     * or if {@code session.getSessionId()} or {@code session.getDeviceFingerPrint()} are {@code null}.
     */
    public void cacheSessionFingerPrint(ClientSession session) {
        Objects.requireNonNull(session, "ClientSession cannot be null when caching fingerprint.");
        Objects.requireNonNull(session.getSessionId(), "Session ID cannot be null when caching fingerprint.");
        Objects.requireNonNull(session.getDeviceFingerPrint(), "Device fingerprint cannot be null when caching.");

        log.debug("Caching session fingerprint for sessionId={}", session.getSessionId());
        sessionFingerprintsCache.cache(session.getSessionId(), session.getDeviceFingerPrint());
    }

    /**
     * Retrieves a session's device fingerprint from the cache using its session ID.
     *
     * @param sessionId The unique identifier of the session whose fingerprint is to be retrieved.
     * Cannot be {@code null} or blank.
     * @return The device fingerprint string associated with the session ID, or {@code null}
     * if the fingerprint is not found in the cache or if the {@code sessionId} is invalid.
     */
    public String getSessionFingerPrint(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Attempt to retrieve session fingerprint with null or blank sessionId. Returning null.");
            return null;
        }
        log.debug("Retrieving session fingerprint for sessionId={}", sessionId);
        return sessionFingerprintsCache.get(sessionId, String.class);
    }

    /**
     * Evicts (removes) a session's device fingerprint from the cache.
     * If the specified session ID does not exist in the cache, no action is performed.
     *
     * @param sessionId The unique identifier of the session whose fingerprint is to be evicted.
     * Cannot be {@code null} or blank.
     */
    public void evictSessionFingerPrint(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Attempt to evict session fingerprint with null or blank sessionId. Aborted.");
            return;
        }
        log.debug("Evicting session fingerprint for sessionId={}", sessionId);
        sessionFingerprintsCache.evict(sessionId);
    }

    /**
     * Evicts multiple session fingerprints from the cache in a single operation.
     * This method is designed for efficiency when clearing many fingerprints at once.
     * If the provided set of {@code sessionIds} is {@code null} or empty, no action is performed.
     *
     * @param sessionIds A {@link Set} of session IDs whose fingerprints are to be evicted.
     * Can be {@code null} or empty.
     */
    public void evictAllSessions(Set<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            log.warn("Attempt to evict sessions with null or empty sessionIds. Aborted.");
            return;
        }
        log.debug("Evicting all session fingerprints for {} sessions.", sessionIds.size());
        sessionFingerprintsCache.evictAll(sessionIds);
    }
}