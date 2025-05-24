package vaultiq.session.cache.service.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.SessionIds;
import vaultiq.session.cache.model.VaultiqSessionCacheEntry;
import vaultiq.session.cache.util.CacheHelper;
import vaultiq.session.cache.util.CacheKeyResolver;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;

import java.util.*;
import java.util.stream.Collectors;

import static vaultiq.session.cache.util.CacheKeyResolver.keyForSession;
import static vaultiq.session.cache.util.CacheKeyResolver.keyForUserSessionMapping;

/**
 * <p>Service for managing {@link VaultiqSession} objects within the application's cache layer.</p>
 *
 * <p>This service provides core functionalities for caching session data, including
 * creating new sessions, retrieving individual or user-specific sessions, and deleting sessions.
 * It also manages the mapping between user IDs and their associated active sessions.</p>
 *
 * <p>The service is conditionally active, meaning it will only be instantiated by Spring
 * if the application's session model is configured to use caching ({@link VaultiqPersistenceMethod#USE_CACHE}
 * for {@link ModelType#SESSION}).</p>
 */
@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.SESSION)
public class VaultiqSessionCacheService {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionCacheService.class);

    private final DeviceFingerprintGenerator fingerprintGenerator;
    private final CacheHelper sessionPoolCache;
    private final SessionRevocationCacheService sessionRevocationCacheService;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs the {@code VaultiqSessionCacheService} with its required dependencies.
     *
     * @param fingerprintGenerator          The utility for generating device fingerprints. Cannot be null.
     * @param sessionPoolCache              The {@link CacheHelper} specifically for the session pool cache. Cannot be null.
     * @param sessionRevocationCacheService The service to check for revoked sessions. Cannot be null.
     */
    public VaultiqSessionCacheService(
            DeviceFingerprintGenerator fingerprintGenerator,
            @Qualifier(CacheHelper.BeanNames.SESSION_POOL_CACHE_HELPER) CacheHelper sessionPoolCache,
            SessionRevocationCacheService sessionRevocationCacheService) {
        this.fingerprintGenerator = Objects.requireNonNull(fingerprintGenerator, "DeviceFingerprintGenerator cannot be null.");
        this.sessionPoolCache = Objects.requireNonNull(sessionPoolCache, "SessionPoolCacheHelper cannot be null.");
        this.sessionRevocationCacheService = Objects.requireNonNull(sessionRevocationCacheService, "SessionRevocationCacheService cannot be null.");
    }

    // -------------------------------------------------------------------------
    // Public API - Session Creation and Caching
    // -------------------------------------------------------------------------

    /**
     * Creates and caches a new session for a given user and HTTP request.
     * The new session is added to the session pool and mapped to the user's sessions.
     *
     * @param userId  The identifier of the user for whom the session is created. Cannot be null or blank.
     * @param request The {@link HttpServletRequest} used to generate a device fingerprint for the session. Cannot be null.
     * @return The newly created {@link VaultiqSession}.
     * @throws NullPointerException     if {@code request} is null.
     * @throws IllegalArgumentException if {@code userId} is null or blank.
     */
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        validateUserId(userId); // Handles null/blank userId validation and throws
        Objects.requireNonNull(request, "HttpServletRequest cannot be null when creating session.");

        String fingerprint = fingerprintGenerator.generateFingerprint(request);
        VaultiqSessionCacheEntry entry = VaultiqSessionCacheEntry.create(userId, fingerprint);

        log.info("Creating session for userId='{}', deviceFingerprint='{}', sessionId='{}'", userId, fingerprint, entry.getSessionId());
        sessionPoolCache.cache(keyForSession(entry.getSessionId()), entry);
        mapNewSessionIdToUser(userId, entry);

        return toVaultiqSession(entry);
    }

    /**
     * Caches or updates an existing {@link VaultiqSession} in the session pool.
     *
     * @param session The {@link VaultiqSession} object to cache. Cannot be null and must have a non-null sessionId.
     * @throws NullPointerException if {@code session} or its sessionId is null.
     */
    public void cacheSession(VaultiqSession session) {
        Objects.requireNonNull(session, "VaultiqSession cannot be null for caching.");
        Objects.requireNonNull(session.getSessionId(), "Session ID cannot be null for caching VaultiqSession.");

        log.debug("Caching session with sessionId='{}'", session.getSessionId());
        VaultiqSessionCacheEntry entry = VaultiqSessionCacheEntry.copy(session);
        sessionPoolCache.cache(keyForSession(session.getSessionId()), entry);
    }

    /**
     * Caches multiple sessions for a user and updates the user-to-session mapping in batch.
     * Each individual session is cached, and then the user's session ID list is updated.
     *
     * @param userId   The identifier of the user. Cannot be null or blank.
     * @param sessions A {@link List} of {@link VaultiqSession} objects to cache. Cannot be null or empty.
     * @throws NullPointerException     if {@code sessions} is null.
     * @throws IllegalArgumentException if {@code userId} is null or blank.
     */
    public void cacheUserSessions(String userId, List<VaultiqSession> sessions) {
        validateUserId(userId); // Handles null/blank userId validation
        Objects.requireNonNull(sessions, "List of sessions cannot be null for caching user sessions.");
        if (sessions.isEmpty()) {
            log.warn("Attempt to cache empty list of sessions for userId='{}'. Skipping.", userId);
            return;
        }

        log.info("Caching {} sessions for userId='{}' in batch.", sessions.size(), userId);
        sessions.forEach(this::cacheSession); // Cache individual sessions

        // Update the user-to-session mapping with all provided session IDs
        Set<String> newSessionIds = sessions.stream().map(VaultiqSession::getSessionId).collect(Collectors.toSet());
        SessionIds currentSessionIds = getUserSessionIds(userId).orElseGet(SessionIds::new);

        // Add all new IDs to the existing set
        newSessionIds.forEach(currentSessionIds::addSessionId); // SessionIds wrapper manages lastUpdated automatically

        sessionPoolCache.cache(keyForUserSessionMapping(userId), currentSessionIds);
        log.info("Successfully cached {} sessions and updated mapping for userId='{}'.", sessions.size(), userId);
    }

    // -------------------------------------------------------------------------
    // Public API - Session Retrieval
    // -------------------------------------------------------------------------

    /**
     * Retrieves a {@link VaultiqSession} from the cache by its ID.
     *
     * @param sessionId The unique ID of the session to retrieve. Cannot be null or blank.
     * @return The {@link VaultiqSession} object if found, otherwise {@code null}.
     */
    public VaultiqSession getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Attempt to retrieve session with null or blank sessionId. Returning null.");
            return null;
        }
        log.debug("Retrieving sessionId='{}' from session pool cache.", sessionId);
        var entry = sessionPoolCache.get(keyForSession(sessionId), VaultiqSessionCacheEntry.class);
        return entry == null ? null : toVaultiqSession(entry);
    }

    /**
     * Retrieves all sessions associated with a specific user from the cache.
     * This method efficiently fetches all session entries using a batch operation.
     *
     * @param userId The identifier of the user. Cannot be null or blank.
     * @return A {@link List} of {@link VaultiqSession} objects for the user. Returns an empty list if no sessions are found.
     */
    public List<VaultiqSession> getSessionsByUser(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempt to get sessions for null or blank userId. Returning empty list.");
            return Collections.emptyList();
        }

        Set<String> userSessionIds = getUserSessionIds(userId)
                .map(SessionIds::getSessionIds)
                .orElse(Collections.emptySet()); // Use emptySet for immutability

        if (userSessionIds.isEmpty()) {
            log.debug("No session IDs found for userId='{}' in mapping. Returning empty list.", userId);
            return Collections.emptyList();
        }

        // Efficiently fetch all session entries in a single batch
        Set<VaultiqSessionCacheEntry> sessionEntries = sessionPoolCache.getAll(userSessionIds, VaultiqSessionCacheEntry.class);

        List<VaultiqSession> sessions = sessionEntries.stream()
                .filter(Objects::nonNull) // Filter out any null entries if exists
                .map(this::toVaultiqSession)
                .collect(Collectors.toList());

        log.debug("Fetched {} sessions for userId='{}'.", sessions.size(), userId);
        return sessions;
    }

    /**
     * Retrieves all active (non-revoked) sessions for a given user from the cache.
     * This method filters the user's sessions by checking their revocation status.
     *
     * @param userId The identifier of the user. Cannot be null or blank.
     * @return A {@link List} of active {@link VaultiqSession} objects. Returns an empty list if no active sessions are found.
     */
    public List<VaultiqSession> getActiveSessionsByUser(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempt to get active sessions for null or blank userId. Returning empty list.");
            return Collections.emptyList();
        }
        return getSessionsByUser(userId).stream()
                .filter(session -> !sessionRevocationCacheService.isSessionRevoked(session.getSessionId()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the total number of sessions for a given user from the cache.
     *
     * @param userId The identifier of the user. Cannot be null or blank.
     * @return The number of sessions found for the user, or 0 if userId is invalid or no sessions are mapped.
     */
    public int totalUserSessions(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempt to get total sessions for null or blank userId. Returning 0.");
            return 0;
        }
        return getUserSessionIds(userId).map(s -> s.getSessionIds().size()).orElse(0);
    }

    // -------------------------------------------------------------------------
    // Public API - Session Deletion
    // -------------------------------------------------------------------------

    /**
     * Deletes a session from the cache and removes its ID from the associated user's session mapping.
     * No action is taken if the session ID is null/blank or the session is not found.
     *
     * @param sessionId The ID of the session to delete. Cannot be null or blank.
     */
    public void deleteSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Attempt to delete session with null or blank sessionId. Skipping.");
            return;
        }

        VaultiqSession session = getSession(sessionId);
        if (session == null) {
            log.debug("Session with sessionId='{}' not found in cache for deletion. Skipping.", sessionId);
            return;
        }

        log.info("Deleting session with sessionId='{}' for userId='{}'.", sessionId, session.getUserId());
        sessionPoolCache.evict(keyForSession(sessionId)); // Evict individual session entry

        // Update user's session ID mapping
        getUserSessionIds(session.getUserId()).ifPresent(ids -> {
            if (ids.removeSessionId(sessionId)) { // SessionIds wrapper manages lastUpdated automatically
                sessionPoolCache.cache(keyForUserSessionMapping(session.getUserId()), ids);
                log.debug("Removed sessionId='{}' from user '{}'s session mapping.", sessionId, session.getUserId());
            } else {
                log.debug("Session ID '{}' not found in user '{}'s mapping during delete (might have been removed already).", sessionId, session.getUserId());
            }
        });
    }

    /**
     * Deletes multiple specified sessions from the cache in a batch operation.
     * This method evicts individual session entries from the cache and then updates
     * the user-to-session mappings for all affected users.
     *
     * @param sessionIds A {@link Set} of session identifiers to delete. Cannot be null or empty.
     * @throws NullPointerException     if {@code sessionIds} is null.
     * @throws IllegalArgumentException if {@code sessionIds} is empty.
     */
    public void deleteAllSessions(Set<String> sessionIds) {
        Objects.requireNonNull(sessionIds, "Set of session IDs cannot be null for deleteAllSessions.");
        if (sessionIds.isEmpty()) {
            log.warn("Attempt to delete all sessions with empty sessionIds. Skipping.");
            return;
        }

        // Filter sessionIds and Prefix with the key
        Set<String> cacheKeysToDelete = sessionIds.stream().filter(Objects::nonNull)
                .map(CacheKeyResolver::keyForSession).collect(Collectors.toSet());

        // Get all session entries to be deleted *before* eviction, to get their user IDs
        var deletableSessionEntries = sessionPoolCache.getAll(cacheKeysToDelete, VaultiqSessionCacheEntry.class);

        log.info("Attempting to delete {} sessions via cache in batch.", cacheKeysToDelete.size());

        // Evict all session entries from the cache
        sessionPoolCache.evictAll(cacheKeysToDelete);
        removeSessionIdsFromUserSessionMapping(deletableSessionEntries);
        log.info("Successfully deleted {} sessions from cache.", cacheKeysToDelete.size());
    }

    // -------------------------------------------------------------------------
    // Internal Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Retrieves the cached set of active session IDs for a user.
     *
     * @param userId The identifier of the user. Cannot be null or blank.
     * @return An {@link Optional} containing the {@link SessionIds} object if found, otherwise an empty Optional.
     */
    public Optional<SessionIds> getUserSessionIds(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempt to get user session IDs for null or blank userId. Returning empty optional.");
            return Optional.empty();
        }
        return Optional.ofNullable(sessionPoolCache.get(keyForUserSessionMapping(userId), SessionIds.class));
    }

    /**
     * Converts a {@link VaultiqSessionCacheEntry} (internal cache representation)
     * into a {@link VaultiqSession} model object (public domain model).
     *
     * @param entry The {@link VaultiqSessionCacheEntry} to convert. Cannot be null.
     * @return The corresponding {@link VaultiqSession} model.
     * @throws NullPointerException if the {@code entry} is null.
     */
    private VaultiqSession toVaultiqSession(VaultiqSessionCacheEntry entry) {
        Objects.requireNonNull(entry, "VaultiqSessionCacheEntry cannot be null for conversion.");
        return VaultiqSession.builder()
                .sessionId(entry.getSessionId())
                .userId(entry.getUserId())
                .deviceFingerPrint(entry.getDeviceFingerPrint())
                .createdAt(entry.getCreatedAt())
                .isBlocked(entry.isRevoked())
                .blockedAt(entry.getRevokedAt())
                .build();
    }

    /**
     * Updates or creates the user-to-session mapping in the cache after a new session is added.
     * This helper ensures that a user's set of session IDs is kept current.
     *
     * @param userId The identifier of the user.
     * @param entry  The {@link VaultiqSessionCacheEntry} to associate with the user.
     */
    private void mapNewSessionIdToUser(String userId, VaultiqSessionCacheEntry entry) {
        SessionIds sessionIds = getUserSessionIds(userId).orElseGet(SessionIds::new);
        sessionIds.addSessionId(entry.getSessionId()); // SessionIds wrapper manages lastUpdated automatically
        sessionPoolCache.cache(keyForUserSessionMapping(userId), sessionIds);
        log.debug("Mapped new sessionId='{}' to user='{}'", entry.getSessionId(), userId);
    }

    /**
     * Helper method to remove all session IDs from the user's session mapping after deletion.
     * This method is called after deleting sessions to ensure the user's session mapping is updated correctly.
     *
     * @param sessions The set of sessions to be removed. Cannot be null or empty.
     */
    private void removeSessionIdsFromUserSessionMapping(Set<VaultiqSessionCacheEntry> sessions) {
        if (sessions == null || sessions.isEmpty()) { // Add null/empty check for robustness
            log.debug("No sessions provided to remove from user session mapping. Skipping.");
            return;
        }

        // Group the raw session IDs by userId
        Map<String, Set<String>> sessionIdsByUser = sessions.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        VaultiqSessionCacheEntry::getUserId,
                        Collectors.mapping(VaultiqSessionCacheEntry::getSessionId, Collectors.toSet())
                ));

        // For each user, update their SessionIds wrapper
        sessionIdsByUser.forEach((userId, idsToDelete) -> {
            getUserSessionIds(userId).ifPresent(userSessionIdsWrapper -> {
                if (userSessionIdsWrapper.removeSessionIds(idsToDelete)) { // Assuming removeSessionIds returns boolean
                    sessionPoolCache.cache(keyForUserSessionMapping(userId), userSessionIdsWrapper);
                    log.debug("Updated session mapping for user '{}': removed {} session(s).", userId, idsToDelete.size());
                } else {
                    log.debug("No session IDs from {} found in user '{}'s mapping to remove.", idsToDelete.size(), userId);
                }
            });
        });
    }

    /**
     * Validates if a user ID is not null or blank.
     *
     * @param userId The user ID to validate.
     * @throws IllegalArgumentException if the {@code userId} is null or blank.
     */
    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank.");
        }
    }
}