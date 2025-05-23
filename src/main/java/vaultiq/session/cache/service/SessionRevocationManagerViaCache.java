
package vaultiq.session.cache.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.cache.model.RevokedSessionCacheEntry;
import vaultiq.session.cache.service.internal.SessionRevocationCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.SessionRevocationManager;
import vaultiq.session.core.model.RevokedSession;
import vaultiq.session.core.model.RevocationRequest;
import vaultiq.session.context.VaultiqSessionContext;

import java.util.List;

/**
 * Implementation of {@link SessionRevocationManager} that manages the revoke state via an in-memory or distributed cache.
 * <p>
 * This implementation is enabled only when Vaultiq is configured for cache-only revoke management
 * ({@code VaultiqPersistenceMode.CACHE_ONLY} for {@code ModelType.REVOKE}).
 * Delegates all business logic to an injected {@link SessionRevocationCacheService}.
 *
 * @see SessionRevocationCacheService
 */
@Service
@ConditionalOnBean(SessionRevocationCacheService.class)
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.CACHE_ONLY, type = ModelType.REVOKE)
public class SessionRevocationManagerViaCache implements SessionRevocationManager {
    private static final Logger log = LoggerFactory.getLogger(SessionRevocationManagerViaCache.class);
    private final SessionRevocationCacheService cacheService;

    /**
     * Constructs the manager with the required cache-based revoke service.
     *
     * @param cacheService Cache-backed revoke handler (must not be null)
     */
    public SessionRevocationManagerViaCache(SessionRevocationCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * revoke sessions based on the provided RevocationRequest.
     *
     * @param revocationRequest the RevocationRequest describing the revoke operation
     */
    @Override
    public void revoke(RevocationRequest revocationRequest) {
        log.debug("Revoking sessions with Revocation type/mode: {}", revocationRequest.getRevocationType().name());
        cacheService.revoke(revocationRequest);
    }

    /**
     * Determine if the session is present in the revoke.
     * Delegates to the cache service.
     *
     * @param sessionId session id to check
     * @return true if session is revoked
     */
    @Override
    public boolean isSessionRevoked(String sessionId) {
        log.debug("Checking if session is revoked: {}", sessionId);
        return cacheService.isSessionRevoked(sessionId);
    }

    /**
     * Retrieve all revoked session ids for a user.
     * Delegates to the cache service.
     *
     * @param userId the user identifier
     * @return list of revoked sessions (never null)
     */
    @Override
    public List<RevokedSession> getRevokedSessions(String userId) {
        log.debug("Getting revoked sessions for user: {}", userId);

        return cacheService.getRevokedSessions(userId)
                .stream()
                .map(this::toSessionRevoke)
                .toList();
    }

    /**
     * Clear the revoke for a specific session or multiple sessions.
     * Delegates to the cache service.
     *
     * @param sessionIds an array of unique sessions identifiers to clear. Can be empty. It Can be blank.
     */
    @Override
    public void clearRevocation(String... sessionIds) {
        log.debug("Attempting to clear revoke for {} sessions", sessionIds.length);
        cacheService.clearRevocation(sessionIds);
    }

    /**
     * Helper method to convert a cache entry to a RevokedSession object.
     *
     * @param entry the cache entry to convert
     * @return the converted RevokedSession object
     */
    private RevokedSession toSessionRevoke(RevokedSessionCacheEntry entry) {
        return RevokedSession.builder()
                .sessionId(entry.getSessionId())
                .userId(entry.getUserId())
                .revocationType(entry.getRevocationType())
                .note(entry.getNote())
                .triggeredBy(entry.getTriggeredBy())
                .revokedAt(entry.getRevokedAt())
                .build();
    }
}