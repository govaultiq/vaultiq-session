package vaultiq.session.cache.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.RevokedSessionCacheEntry;
import vaultiq.session.cache.service.internal.RevokedSIdCacheService;
import vaultiq.session.cache.service.internal.SessionRevocationCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.domain.contracts.internal.SessionRevocationManager;
import vaultiq.session.domain.model.ModelType;
import vaultiq.session.domain.model.RevocationRequest;
import vaultiq.session.domain.model.RevokedSession;

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
    private final RevokedSIdCacheService revokedSIdCacheService;

    /**
     * Constructs the manager with the required cache-based revoke service.
     *
     * @param cacheService Cache-backed revoke handler (must not be null)
     */
    public SessionRevocationManagerViaCache(
            SessionRevocationCacheService cacheService,
            RevokedSIdCacheService revokedSIdCacheService
    ) {
        this.cacheService = cacheService;
        this.revokedSIdCacheService = revokedSIdCacheService;
    }

    /**
     * revoke sessions based on the provided RevocationRequest.
     *
     * @param revocationRequest the RevocationRequest describing the revoke operation
     */
    @Override
    public void revoke(RevocationRequest revocationRequest) {
        log.debug("Revoking sessions with Revocation type/mode: {}", revocationRequest.getRevocationType().name());
        var sessionIdSet = cacheService.revoke(revocationRequest);
        if (sessionIdSet != null && !sessionIdSet.isEmpty()) {
            // Updating the same revoked sessionId list to the revoked-sids cache
            revokedSIdCacheService.revoke(sessionIdSet);
        } else {
            // else revoke independently in revoke-sids cache
            revokedSIdCacheService.revoke(revocationRequest);
        }
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
        if (revokedSIdCacheService.isSessionRevoked(sessionId)) return true;
        else if (cacheService.isSessionRevoked(sessionId)) {
            // auto-healing
            revokedSIdCacheService.revoke(sessionId);
            return true;
        } else return false;
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