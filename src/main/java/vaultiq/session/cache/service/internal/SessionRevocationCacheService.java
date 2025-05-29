package vaultiq.session.cache.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.util.CacheHelper;
import vaultiq.session.cache.model.RevokedSessionCacheEntry;
import vaultiq.session.cache.model.SessionIds;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.core.SessionManager;
import vaultiq.session.core.contracts.UserIdentityAware;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.core.model.RevocationType;
import vaultiq.session.core.model.RevocationRequest;
import vaultiq.session.core.model.VaultiqSession;

import java.util.*;
import java.util.stream.Collectors;

import static vaultiq.session.cache.util.CacheKeyResolver.*;

/**
 * <p>Service for managing revoked (invalidated) sessions within the in-memory cache layer.
 * This service provides fast lookups and revoke operations for sessions, supporting
 * various strategies such as revoking single or multiple sessions, revoking all sessions
 * except specified ones, and querying for a user's currently revoked sessions.</p>
 *
 * <p>This service leverages {@link CacheHelper} for conditional cache interactions,
 * ensuring operations silently skip if the underlying cache is not configured.
 * It interacts with {@link SessionManager} to manage active sessions,
 * ensuring that revoked sessions are also removed from the active session store.</p>
 *
 * <p>This service is conditionally enabled by {@code @ConditionalOnVaultiqModelConfig},
 * meaning it will only be active if the application's persistence method is
 * {@link VaultiqPersistenceMethod#USE_CACHE} for {@link ModelType#REVOKE} data.</p>
 */
@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.REVOKE)
public class SessionRevocationCacheService {

    private static final Logger log = LoggerFactory.getLogger(SessionRevocationCacheService.class);

    private final SessionManager sessionManager;
    private final UserIdentityAware userIdentityAware;
    private final CacheHelper revocationPoolCache;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a cache-backed revoke service for sessions.
     *
     * @param sessionManager The manager for active session operations, ensuring data consistency
     *                              across persistence strategies. Cannot be null.
     * @param userIdentityAware     Used for audit context (marking who triggers revokes). Cannot be null.
     * @param revocationPoolCache   The {@link CacheHelper} instance specifically for the revoked session pool cache.
     *                              This is autowired by Spring using its bean name. Cannot be null.
     */
    public SessionRevocationCacheService(
            SessionManager sessionManager,
            UserIdentityAware userIdentityAware,
            @Qualifier(CacheHelper.BeanNames.REVOKED_SESSION_POOL_CACHE_HELPER)
            CacheHelper revocationPoolCache
    ) {
        this.sessionManager = Objects.requireNonNull(sessionManager, "SessionManager bean not found.");
        this.userIdentityAware = Objects.requireNonNull(userIdentityAware, "UserIdentityAware bean not found.");
        this.revocationPoolCache = Objects.requireNonNull(revocationPoolCache, "RevocationPoolCacheHelper bean not found.");
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Dispatches a revoke operation according to the revocation strategy defined in the provided context.
     *
     * @param context Describes what to revoke and how. If null, no operation is performed.
     */
    public Set<String> revoke(RevocationRequest context) {
        if (context == null) {
            log.debug("RevocationRequest context is null. Skipping revoke operation.");
            return Collections.emptySet();
        }
        return switch (context.getRevocationType()) {
            case LOGOUT -> revokeSession(context.getIdentifier(), context.getNote());
            case LOGOUT_ALL -> revokeAllSessions(context.getIdentifier(), context.getNote());
            case LOGOUT_WITH_EXCLUSION -> revokeWithExclusions(
                    context.getIdentifier(),
                    context.getNote(),
                    context.getExcludedSessionIds()
            );
        };
    }

    /**
     * Revokes a single session by storing its entry in the cache and deleting the session from the active session store.
     * This method is intended for direct use when a {@link RevokedSessionCacheEntry} is already formed.
     *
     * @param entry The revoked session entry to persist. Cannot be null.
     */
    public void revoke(RevokedSessionCacheEntry entry) {
        Objects.requireNonNull(entry, "RevokedSessionCacheEntry cannot be null.");
        // This internal method handles the actual caching and session deletion
        putRevokedSessionEntry(entry);
        log.info("Session with sessionId='{}' revoked.", entry.getSessionId());
    }

    /**
     * Revokes a single active session by its ID and a descriptive note.
     * It first checks if the session exists in the active store and if it's already revoked.
     *
     * @param sessionId The session's identifier. Cannot be null or blank.
     * @param note      The note/reason for revoking the session. Can be null.
     * @return {@code Set<String>} containing the revoked session ID. Returns an empty set if no session is found.
     */
    public Set<String> revokeSession(String sessionId, String note) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Attempt to revoke session with null or blank sessionId. Skipping.");
            return Collections.emptySet();
        }

        VaultiqSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            log.debug("Attempt to revoke non-existent active session: {}. Skipping.", sessionId);
            return Collections.emptySet();
        }

        if (isSessionRevoked(sessionId)) {
            log.debug("Session with sessionId='{}' is already revoked. Skipping re-revoke.", sessionId);
            return Collections.emptySet();
        }

        var entry = createRevokedSessionCacheEntry(session, note, RevocationType.LOGOUT);
        putRevokedSessionEntry(entry);
        log.info("Session '{}' for user '{}' successfully revoked.", sessionId, session.getUserId());
        return Set.of(sessionId);
    }

    /**
     * Revokes all active sessions for a given user.
     *
     * @param userId The user whose sessions should be revoked. Cannot be null or blank.
     * @param note   The note/reason for revoking. Can be null.
     * @return {@code Set<String>} containing the revoked session IDs. Returns an empty set if no sessions are found.
     */
    public Set<String> revokeAllSessions(String userId, String note) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempt to revoke all sessions for null or blank userId. Skipping.");
            return Collections.emptySet();
        }
        List<VaultiqSession> sessions = sessionManager.getActiveSessionsByUser(userId);
        var sessionIdSet = revokeSessionSet(userId, note, sessions, RevocationType.LOGOUT_ALL);
        log.info("Successfully initiated revoke all for user '{}' affecting {} sessions.", userId, sessions.size());
        return sessionIdSet;
    }

    /**
     * Revokes all active sessions for a user except those specified as excluded.
     *
     * @param userId             The user whose sessions to revoke. Cannot be null or blank.
     * @param note               The note/reason for revoking. Can be null.
     * @param excludedSessionIds A set of session IDs to be excluded from revoking. Can be null or empty.
     * @return {@code Set<String>} containing the revoked session IDs. Returns an empty set if no sessions are found.
     */
    public Set<String> revokeWithExclusions(String userId, String note, Set<String> excludedSessionIds) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempt to revoke with exclusions for null or blank userId. Skipping.");
            return Collections.emptySet();
        }

        var filteredSessions = sessionManager.getActiveSessionsByUser(userId).stream()
                .filter(session -> !excludedSessionIds.contains(session.getSessionId()))
                .toList();

        var sessionIdSet = revokeSessionSet(userId, note, filteredSessions, RevocationType.LOGOUT_WITH_EXCLUSION);
        log.info("Successfully initiated revoke with exclusions for user '{}', affecting {} sessions.", userId, filteredSessions.size());

        return sessionIdSet;
    }

    /**
     * Checks if a session is currently marked as revoked in the cache.
     *
     * @param sessionId The session identifier. Cannot be null or blank.
     * @return {@code true} if revoked, {@code false} otherwise.
     */
    public boolean isSessionRevoked(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Attempt to check revocation status for null or blank sessionId. Returning false.");
            return false;
        }
        return revocationPoolCache.get(keyForRevocation(sessionId), RevokedSessionCacheEntry.class) != null;
    }

    /**
     * Retrieves the set of revoked session IDs for a specific user from the cache.
     *
     * @param userId The user whose revoked session IDs to fetch. Cannot be null or blank.
     * @return A {@link SessionIds} object containing IDs and cache update timestamp, or {@code null} if not found.
     */
    public SessionIds getRevokedSessionIds(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempt to get revoked session IDs for null or blank userId. Returning null.");
            return null;
        }
        return revocationPoolCache.get(keyForRevocationByUser(userId), SessionIds.class);
    }

    /**
     * Retrieves all revoked session entries for a user from the cache.
     * This method efficiently fetches all associated {@link RevokedSessionCacheEntry} objects
     * by using a batch retrieval mechanism from the {@link CacheHelper}.
     *
     * @param userId The user's identifier. Cannot be null or blank.
     * @return A {@link List} of {@link RevokedSessionCacheEntry} for the user. Returns an empty list if no revoked sessions are found.
     */
    public List<RevokedSessionCacheEntry> getRevokedSessions(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempt to get revoked sessions for null or blank userId. Returning empty list.");
            return Collections.emptyList();
        }
        SessionIds sessionIdsWrapper = getRevokedSessionIds(userId);
        if (sessionIdsWrapper == null || sessionIdsWrapper.getSessionIds() == null || sessionIdsWrapper.getSessionIds().isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> revokedSessionIds = sessionIdsWrapper.getSessionIds();
        Map<String, RevokedSessionCacheEntry> revokedEntriesMap =
                revocationPoolCache.getAllAsMap(revokedSessionIds, RevokedSessionCacheEntry.class);

        return revokedEntriesMap.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Gets the last updated timestamp for the revocation status of a given user.
     *
     * @param userId The user for whom to fetch the last updated time. Cannot be null or blank.
     * @return An {@link Optional} containing the last updated timestamp if present, otherwise an empty Optional.
     */
    public Optional<Long> getLastUpdatedAt(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempt to get last updated at for null or blank userId. Returning empty optional.");
            return Optional.empty();
        }
        var sessionIds = getRevokedSessionIds(userId);
        return Optional.ofNullable(sessionIds).map(SessionIds::getLastUpdated);
    }

    // -------------------------------------------------------------------------
    // Internal Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Helper method to revoke all active sessions for a user. This method iterates through all active sessions
     * and marks them as revoked.
     *
     * @param userId         The user whose sessions should be revoked. Cannot be null or blank.
     * @param note           The note/reason for revoking. Can be null.
     * @param sessions       A list of active sessions to be revoked. Cannot be null.
     * @param revocationType The type of revocation (e.g., LOGOUT, LOGOUT_ALL, LOGOUT_WITH_EXCLUSION). Cannot be null.
     */
    private Set<String> revokeSessionSet(String userId, String note, List<VaultiqSession> sessions, RevocationType revocationType) {
        if (sessions.isEmpty()) {
            log.info("No active sessions found for user '{}' to revoke all. Skipping.", userId);
            return Collections.emptySet();
        }

        sessions.forEach(session -> {
            var entry = this.createRevokedSessionCacheEntry(session, note, revocationType);
            putRevokedSessionEntry(entry);
        });

        return sessions.stream().map(VaultiqSession::getSessionId).collect(Collectors.toSet());
    }

    /**
     * Helper to add a revoked session entry to the cache and remove the session from the active session cache.
     * Also updates the mapping of revoked sessions for the user.
     *
     * @param entry The revoked session entry to persist. Cannot be null.
     */
    private void putRevokedSessionEntry(RevokedSessionCacheEntry entry) {
        Objects.requireNonNull(entry, "RevokedSessionCacheEntry for putting cannot be null");
        String userId = entry.getUserId();
        String sessionId = entry.getSessionId();

        revocationPoolCache.cache(keyForRevocation(sessionId), entry);
        // TODO: This may not be a good Idea,
        //  if the session have
        //  to persist even after revoked
        //  so updating session can be better Idea
        //  than deleting it from active session as it not only removes from cache,
        //  but also in the DB.
        sessionManager.deleteSession(sessionId);

        SessionIds userRevokedSessionIds = Optional.ofNullable(getRevokedSessionIds(userId))
                .orElseGet(SessionIds::new);
        userRevokedSessionIds.addSessionId(sessionId);
        revocationPoolCache.cache(keyForRevocationByUser(userId), userRevokedSessionIds);

        log.debug("Revoked session entry for sessionId='{}' and userId='{}' updated in cache.", sessionId, userId);
    }

    /**
     * Helper method to create a revoked session cache entry based on the provided session details.
     *
     * @param session The session object to create the entry for. Cannot be null.
     * @param note    The note/reason for revoking the session. Can be null.
     * @return A new {@link RevokedSessionCacheEntry} object. Cannot be null.
     */
    private RevokedSessionCacheEntry createRevokedSessionCacheEntry(VaultiqSession session, String note, RevocationType revocationType) {
        return RevokedSessionCacheEntry.create(
                session.getSessionId(),
                session.getUserId(), // Use the userId from the retrieved active session
                revocationType, // Assuming LOGOUT for single session revoke via this method
                note,
                userIdentityAware.getCurrentUserID()
        );
    }
}
