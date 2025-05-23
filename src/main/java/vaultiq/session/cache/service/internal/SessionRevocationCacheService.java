
package vaultiq.session.cache.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.util.CacheType;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.cache.model.RevokedSessionCacheEntry;
import vaultiq.session.cache.model.SessionIds;
import vaultiq.session.cache.util.VaultiqCacheContext;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.context.VaultiqSessionContextHolder;
import vaultiq.session.core.contracts.UserIdentityAware;
import vaultiq.session.core.model.RevocationType;
import vaultiq.session.core.model.RevocationRequest;

import java.util.*;
import java.util.stream.Collectors;

import static vaultiq.session.cache.util.CacheKeyResolver.*;

/**
 * Service for managing revoked (invalidated) sessions in the in-memory cache layer.
 * <p>
 * Provides fast lookups and revoke operations for sessions, with support for revoking single or multiple sessions,
 * revoking all sessions except some, and querying for a user's revoked sessions.
 * </p>
 * <b>Note: </b>
 *  The bean of this class is registered conditionally via the {@link vaultiq.session.cache.config.CacheServiceAutoRegistrar}
 *  ensuring the bean is only available if the cache of type {@link CacheType#REVOKED_SESSION_POOL} is available.
 */
public class SessionRevocationCacheService {

    private static final Logger log = LoggerFactory.getLogger(SessionRevocationCacheService.class);

    private final VaultiqSessionCacheService vaultiqSessionCacheService;
    private final Cache revocationPoolCache;
    private final UserIdentityAware userIdentityAware;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a cache-backed revoke service for sessions.
     *
     * @param cacheContext               context holding/initializing cache instances
     * @param vaultiqSessionCacheService backing service for session cache access
     * @param userIdentityAware          used for audit context (marking who triggers revokes)
     */
    public SessionRevocationCacheService(
            VaultiqCacheContext cacheContext,
            VaultiqSessionCacheService vaultiqSessionCacheService,
            UserIdentityAware userIdentityAware
    ) {
        this.userIdentityAware = userIdentityAware;
        var vaultiqModelConfig = VaultiqSessionContextHolder.getContext().getModelConfig(CacheType.REVOKED_SESSION_POOL);
        if (vaultiqModelConfig == null) {
            throw new IllegalArgumentException("Model config not found! Check your configuration for ModelType.REVOKE.");
        }

        this.vaultiqSessionCacheService = Objects.requireNonNull(vaultiqSessionCacheService, "VaultiqSessionCacheService may not be null");
        this.revocationPoolCache = cacheContext.getCacheMandatory(
                vaultiqModelConfig.cacheName(),
                CacheType.REVOKED_SESSION_POOL
        );
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Dispatches a revoke operation according to the revocation strategy in the provided context.
     *
     * @param context describes what to revoke and how
     */
    public void revoke(RevocationRequest context) {
        if (context != null) {
            switch (context.getRevocationType()) {
                case LOGOUT -> revokeSession(context.getIdentifier(), context.getNote());
                case LOGOUT_ALL -> revokeAllSessions(context.getIdentifier(), context.getNote());
                case LOGOUT_WITH_EXCLUSION -> revokeWithExclusions(
                        context.getIdentifier(),
                        context.getNote(),
                        context.getExcludedSessionIds()
                );
            }
        }
    }

    /**
     * revokes a single session by storing an entry in cache.
     *
     * @param entry the session entry to revoke
     */
    public void revoke(RevokedSessionCacheEntry entry) {
        revokeSession(entry);
        vaultiqSessionCacheService.deleteSession(entry.getSessionId());
        log.info("Session with sessionId={} revoked.", entry.getSessionId());
    }

    /**
     * Revokes a single session by sessionId and note.
     *
     * @param sessionId the session's identifier
     * @param note      the note/reason for Revoking the session.
     */
    public void revokeSession(String sessionId, String note) {
        var session = vaultiqSessionCacheService.getSession(sessionId);

        if (session == null) {
            log.info("Attempt to revoke non-existent session: {}", sessionId);
            return;
        }

        if (isSessionRevoked(sessionId)) {
            log.debug("Session with sessionId={} is already revoked.", sessionId);
            return;
        }

        var entry = RevokedSessionCacheEntry.create(
                sessionId,
                session.getUserId(),
                RevocationType.LOGOUT,
                note,
                userIdentityAware.getCurrentUserID()
        );

        revokeSession(entry);
    }

    /**
     * Revokes all sessions for a given user.
     *
     * @param userId the user whose sessions should be revoked
     * @param note   note/reason for revoking
     */
    public void revokeAllSessions(String userId, String note) {
        var sessionIds = vaultiqSessionCacheService.getUserSessionIds(userId);
        revokeSessions(userId, sessionIds, note);
    }

    /**
     * Revokes all sessions for a user except those specified as excluded.
     *
     * @param userId             the user whose sessions to revoke.
     * @param note               note/reason for revoking.
     * @param excludedSessionIds session IDs to be excluded from revoking.
     */
    public void revokeWithExclusions(String userId, String note, Set<String> excludedSessionIds) {

        var sessionIds = vaultiqSessionCacheService.getUserSessionIds(userId).stream()
                .filter(id -> !excludedSessionIds.contains(id))
                .collect(Collectors.toSet());

        if (sessionIds.isEmpty()) {
            log.info("No sessions to revoke for user '{}'; all are excluded.", userId);
        }

        revokeSessions(userId, sessionIds, note);
    }

    /**
     * Checks if a session is currently revoked in the cache.
     *
     * @param sessionId the session identifier
     * @return true if revoked, false otherwise
     */
    public boolean isSessionRevoked(String sessionId) {
        return revocationPoolCache.get(keyForRevocation(sessionId)) != null;
    }

    /**
     * Retrieves the revoked session IDs for a user.
     *
     * @param userId the user whose revoked session IDs to fetch
     * @return a SessionIds object containing IDs and cache update timestamp
     */
    public SessionIds getRevokedSessionIds(String userId) {
        return revocationPoolCache.get(keyForRevocationByUser(userId), SessionIds.class);
    }

    /**
     * Retrieves all revoked session entries for a user from the cache.
     *
     * @param userId the user's identifier
     * @return a list of RevokedSessionCacheEntry for the user
     */
    public List<RevokedSessionCacheEntry> getRevokedSessions(String userId) {
        var sessionIds = getRevokedSessionIds(userId);
        if (sessionIds == null || sessionIds.getSessionIds() == null) {
            return List.of();
        }

        return sessionIds.getSessionIds().stream()
                .map(id -> revocationPoolCache.get(keyForRevocation(id), RevokedSessionCacheEntry.class))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Gets the last updated timestamp for the revoke of a given user.
     *
     * @param userId the user for whom to fetch the last updated time
     * @return an Optional containing the last updated timestamp if present
     */
    public Optional<Long> getLastUpdatedAt(String userId) {
        var sessionIds = getRevokedSessionIds(userId);
        return Optional.ofNullable(sessionIds).map(SessionIds::getLastUpdated);
    }

    /**
     * Removes (unblocks) the revoke entry for a session from the cache.
     *
     * @param sessionId the session identifier to unblock
     */
    public void unblockSession(String sessionId) {
        revocationPoolCache.evict(keyForRevocation(sessionId));
        log.info("Session with sessionId={} unrevoked.", sessionId);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /**
     * Revokes a specific set of session IDs for a user.
     *
     * @param userId     the user's identifier
     * @param sessionIds the set of session IDs to revoke
     * @param note       note/reason for revoking
     */
    private void revokeSessions(String userId, Set<String> sessionIds, String note) {
        if (sessionIds.isEmpty()) {
            log.info("No sessions to revoke for user '{}'.", userId);
        }

        var sessions = getRevokedSessions(userId);

        sessions.forEach(session -> {
            var entry = RevokedSessionCacheEntry.create(
                    session.getSessionId(),
                    session.getUserId(),
                    RevocationType.LOGOUT,
                    note,
                    userIdentityAware.getCurrentUserID()
            );
            revokeSession(entry);
        });
    }

    /**
     * Helper to add a revoked session entry to the cache and remove the session from the session cache.
     * Also updates the mapping of revoked sessions for the user.
     *
     * @param entry the revoke entry to persist
     */
    private void revokeSession(RevokedSessionCacheEntry entry) {
        var userId = entry.getUserId();
        var sessionId = entry.getSessionId();

        revocationPoolCache.put(keyForRevocation(sessionId), entry);
        vaultiqSessionCacheService.deleteSession(sessionId);

        SessionIds sessionIds = Optional.ofNullable(getRevokedSessionIds(userId))
                .orElseGet(SessionIds::new);
        sessionIds.addSessionId(sessionId);
        revocationPoolCache.put(keyForRevocationByUser(userId), sessionIds);

        log.info("User '{}' had session '{}' revoked.", userId, sessionId);
    }

    /**
     * Clears the revoke for a list of session IDs.
     *
     * @param sessionIds the session IDs to clear from the revoke
     */
    public void clearRevocation(String... sessionIds) {
        if (sessionIds == null || sessionIds.length == 0) {
            log.info("No sessions to clear from revoke.");
            return;
        }
        for (String sessionId : sessionIds) {
            revocationPoolCache.evict(keyForRevocation(sessionId));
        }
        log.info("Blocklist cleared for {} sessions: {}", sessionIds.length, Arrays.toString(sessionIds));
    }
}
