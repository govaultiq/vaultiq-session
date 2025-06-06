
package vaultiq.session.jpa.revoke.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.service.internal.RevokedSIdCacheService;
import vaultiq.session.model.ModelType;
import vaultiq.session.cache.model.RevokedSessionCacheEntry;
import vaultiq.session.cache.service.internal.SessionRevocationCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.service.SessionRevocationManager;
import vaultiq.session.core.spi.UserIdentityAware;
import vaultiq.session.model.RevokedSession;
import vaultiq.session.model.RevocationRequest;
import vaultiq.session.jpa.revoke.model.RevokedSessionEntity;
import vaultiq.session.jpa.revoke.service.internal.RevokedSessionEntityService;

import java.util.*;

/**
 * An implementation of the SessionRevocationManager that uses both cache and JPA persistence for session revoking.
 * <p>
 * The Primary preference is given to the cache for retrieving or updating revoke status to achieve maximum performance;
 * a database is used as a fallback and authoritative source to keep the cache updated in case of cache misses or stale data.
 * </p>
 *
 * <p>
 * Typical revoke/write operations update the database and then populate the cache. Read/check operations first attempt from
 * the cache, falling back to JPA when necessary, and update the cache if needed.
 * </p>
 */
@Service
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_AND_CACHE, type = {ModelType.REVOKE, ModelType.SESSION})
public class SessionRevocationManagerViaJpaCacheEnabled implements SessionRevocationManager {
    private static final Logger log = LoggerFactory.getLogger(SessionRevocationManagerViaJpaCacheEnabled.class);

    private final RevokedSessionEntityService revokedSessionEntityService;
    private final SessionRevocationCacheService sessionRevocationCacheService;
    private final RevokedSIdCacheService revokedSIdCacheService;
    private final UserIdentityAware userIdentityAware;

    /**
     * Constructs a new SessionRevocationManagerViaJpaCacheEnabled.
     *
     * @param revokedSessionEntityService the service for JPA-based revoke persistence
     * @param sessionRevocationCacheService  the service for cache-based revoke management
     * @param userIdentityAware             used to acquire the current user for audit purposes
     */
    public SessionRevocationManagerViaJpaCacheEnabled(
            RevokedSessionEntityService revokedSessionEntityService,
            SessionRevocationCacheService sessionRevocationCacheService,
            RevokedSIdCacheService revokedSIdCacheService,
            UserIdentityAware userIdentityAware
    ) {
        this.revokedSessionEntityService = revokedSessionEntityService;
        this.sessionRevocationCacheService = sessionRevocationCacheService;
        this.revokedSIdCacheService = revokedSIdCacheService;
        this.userIdentityAware = userIdentityAware;
        log.info("SessionRevocationManager Initialized; Persistence via - JPA_AND_CACHE.");
    }

    /**
     * Marks sessions as revoked according to the provided request context.
     * Updates the database and, on successful update, the cache is also refreshed.
     *
     * @param request the revoke operation request
     */
    @Override
    public void revoke(RevocationRequest request) {
        log.debug("Blocking sessions with Revocation type/mode: {}", request.getRevocationType().name());
        revokedSessionEntityService.revoke(request);

        var revokedIds = sessionRevocationCacheService.revoke(request);

        // cache Ids directly if whole RevokedSessions are cached
        if(revokedIds != null && !revokedIds.isEmpty())
            revokedSIdCacheService.revoke(revokedIds);
        // else cache using request context
        else
            revokedSIdCacheService.revoke(request);
    }

    /**
     * Checks if a given session is revoked, preferring a cache lookup then falling back to JPA if needed.
     * If revoked in DB but missing in cache, the cache will be updated.
     *
     * @param sessionId the session identifier to check
     * @return true if the session is revoked, false otherwise
     */
    @Override
    public boolean isSessionRevoked(String sessionId) {
        log.debug("Checking if session '{}' is revoked.", sessionId);

        // Check if the session is revoked in the cache
        if (revokedSIdCacheService.isSessionRevoked(sessionId) || sessionRevocationCacheService.isSessionRevoked(sessionId)) {
            return true;
        }

        // If not found in the cache, check the database (JPA)
        if (revokedSessionEntityService.isSessionRevoked(sessionId)) {
            var entity = revokedSessionEntityService.getRevokedSession(sessionId);
            return cacheEntity(entity);
        }

        // If not found in cache or db, return false (session is not revoked)
        return false;
    }

    /**
     * Helper method to cache a RevokedSessionEntity as a RevokedSessionCacheEntry.
     * Also attempts to cache sessionId to revoked-sids
     *
     * @param entity the JPA entity to cache
     * @return always true (for integration as a predicate)
     */
    private boolean cacheEntity(RevokedSessionEntity entity) {
        var newEntry = new RevokedSessionCacheEntry();
        newEntry.setSessionId(entity.getSessionId());
        newEntry.setUserId(entity.getUserId());
        newEntry.setRevocationType(entity.getRevocationType());
        newEntry.setNote(entity.getNote());
        newEntry.setTriggeredBy(userIdentityAware.getCurrentUserID());
        newEntry.setRevokedAt(entity.getRevokedAt());

        sessionRevocationCacheService.revoke(newEntry);
        revokedSIdCacheService.revoke(newEntry.getSessionId());
        return true;
    }

    /**
     * Retrieves all revoked sessions for a given user, checking cache first then merging with the database if the cache is stale.
     *
     * @param userId the user identifier whose revoked sessions are to be obtained
     * @return list of revoked sessions
     */
    @Override
    public List<RevokedSession> getRevokedSessions(String userId) {
        log.debug("Fetching revoked sessions for user '{}'.", userId);

        // Step 1: Fetch from cache
        List<RevokedSession> cacheRevoked = sessionRevocationCacheService.getRevokedSessions(userId)
                .stream()
                .map(this::toSessionRevoked)
                .toList();

        // Step 2: Check if the cache is stale
        var isStale = sessionRevocationCacheService.getLastUpdatedAt(userId)
                .map(lastUpdatedAt -> revokedSessionEntityService.isLastUpdatedAfter(userId, lastUpdatedAt))
                .orElse(false);

        if (!isStale) {
            log.debug("Revoke found from cache. Not stale.");
            return cacheRevoked;
        }

        // Step 3: Cache might be stale/incomplete → fallback to a merged result
        log.debug("Revoke is stale in cache, fetching from DB.");
        List<RevokedSession> revoked = revokedSessionEntityService.getRevokedSessions(userId)
                .stream()
                .map(this::toSessionRevoked)
                .toList();

        // Step 4: Merge, preferring cache
        Map<String, RevokedSession> merged = new LinkedHashMap<>();
        for (RevokedSession bl : revoked) merged.put(bl.getSessionId(), bl);
        for (RevokedSession bl : cacheRevoked) merged.put(bl.getSessionId(), bl); // override with cache

        return new ArrayList<>(merged.values());
    }

    /**
     * Helper method to convert a JPA entity to a RevokedSession object.
     *
     * @param entity the JPA entity {@link RevokedSessionEntity} to convert
     * @return the converted {@link RevokedSession} object
     */
    private RevokedSession toSessionRevoked(RevokedSessionEntity entity) {
        return RevokedSession.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .revocationType(entity.getRevocationType())
                .note(entity.getNote())
                .triggeredBy(entity.getTriggeredBy())
                .revokedAt(entity.getRevokedAt())
                .build();
    }

    /**
     * Helper method to convert a cache entry to a RevokedSession object.
     *
     * @param entry the cache entity {@link RevokedSessionCacheEntry} to convert
     * @return the converted {@link RevokedSession} object
     */
    private RevokedSession toSessionRevoked(RevokedSessionCacheEntry entry) {
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

