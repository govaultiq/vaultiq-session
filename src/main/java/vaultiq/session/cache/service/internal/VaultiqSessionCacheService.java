
package vaultiq.session.cache.service.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.cache.model.SessionIds;
import vaultiq.session.cache.util.VaultiqCacheContext;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.context.VaultiqSessionContext;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.cache.model.VaultiqSessionCacheEntry;

import java.util.*;
import java.util.stream.Collectors;

import static vaultiq.session.cache.util.CacheKeyResolver.keyForSession;
import static vaultiq.session.cache.util.CacheKeyResolver.keyForUserSessionMapping;

/**
 * Provides in-memory or distributed cache-backed session pool operations for Vaultiq sessions.
 * <p>
 * All active sessions, and the mapping of users to their session IDs, are managed in configurable caches.
 * This is the foundational service for "cache-only" session management and is used by {@link vaultiq.session.cache.service.VaultiqSessionManagerViaCache}.
 * </p>
 * <ul>
 *   <li>Fast CRUD for session objects (add, read, remove, a list).</li>
 *   <li>Keeps user-to-session mappings up to date in dedicated caches for efficient user/session lookup.</li>
 *   <li>Supports device fingerprinting for device-specific session tracking.</li>
 * </ul>
 */
@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
public class VaultiqSessionCacheService {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionCacheService.class);

    private final Cache sessionPoolCache;
    private final Cache userSessionMappingCache;
    private final DeviceFingerprintGenerator fingerprintGenerator;

    /**
     * Initializes the cache service using designated caches and a fingerprint generator.
     *
     * @param context              application session context for model-specific configuration
     * @param cacheContext         cache context providing named caches
     * @param fingerprintGenerator device-specific session ID utility
     */
    public VaultiqSessionCacheService(
            VaultiqSessionContext context,
            VaultiqCacheContext cacheContext,
            DeviceFingerprintGenerator fingerprintGenerator) {

        this.sessionPoolCache = cacheContext.getCacheMandatory(context.getModelConfig(ModelType.SESSION).cacheName(), ModelType.SESSION);
        this.userSessionMappingCache = cacheContext.getCacheOptional(
                        context.getModelConfig(ModelType.USER_SESSION_MAPPING).cacheName(),
                        ModelType.USER_SESSION_MAPPING)
                .orFallbackTo(sessionPoolCache, ModelType.SESSION);

        this.fingerprintGenerator = fingerprintGenerator;
    }

    /**
     * Creates and caches a new session for a given user and device request.
     * Updates both the session pool and user-session mapping caches.
     *
     * @param userId  the user identifier
     * @param request HTTP request used for device fingerprinting
     * @return the created session
     */
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        String fingerprint = fingerprintGenerator.generateFingerprint(request);
        VaultiqSessionCacheEntry sessionEntry = VaultiqSessionCacheEntry.create(userId, fingerprint);
        log.info("Creating session for userId={}, deviceFingerprint={}", userId, fingerprint);

        sessionPoolCache.put(keyForSession(sessionEntry.getSessionId()), sessionEntry);
        mapNewSessionIdToUser(userId, sessionEntry);

        log.debug("Session added to pool and cached for userId={}, sessionId={}", userId, sessionEntry.getSessionId());
        return toVaultiqSession(sessionEntry);
    }

    /**
     * Adds or updates a cached session by copying the provided VaultiqSession object.
     *
     * @param vaultiqSession the session to cache
     */
    public void cacheSession(VaultiqSession vaultiqSession) {
        log.info("Caching session with sessionId={}", vaultiqSession.getSessionId());
        VaultiqSessionCacheEntry cacheEntry = VaultiqSessionCacheEntry.copy(vaultiqSession);
        sessionPoolCache.put(keyForSession(vaultiqSession.getSessionId()), cacheEntry);
    }

    /**
     * Retrieves an active session from the session pool cache, converting cache entry to model.
     *
     * @param sessionId the session identifier
     * @return the VaultiqSession object, or null if not found
     */
    public VaultiqSession getSession(String sessionId) {
        var session = Optional.ofNullable(sessionPoolCache.get(keyForSession(sessionId), VaultiqSessionCacheEntry.class))
                .map(this::toVaultiqSession)
                .orElse(null);

        log.debug("Retrieved sessionId={} from session pool, found={}", sessionId, session != null);

        return session;
    }

    /**
     * Deletes a session from the session pool and user-session mapping cache.
     * Updates the mapping if the session exists.
     *
     * @param sessionId the session identifier
     */
    public void deleteSession(String sessionId) {
        log.debug("Deleting session with sessionId={}", sessionId);
        var session = getSession(sessionId);

        if (session != null) {
            sessionPoolCache.evict(keyForSession(sessionId));
            var sessionIds = getUserSessionIds(session.getUserId());
            sessionIds.remove(sessionId);

            SessionIds updated = new SessionIds();
            updated.setSessionIds(sessionIds);
            userSessionMappingCache.put(keyForUserSessionMapping(session.getUserId()), updated);
        }
    }

    /**
     * Retrieves all sessions belonging to a given user, based on cached session IDs.
     *
     * @param userId the user identifier
     * @return list of VaultiqSession objects (maybe empty if none)
     */
    public List<VaultiqSession> getSessionsByUser(String userId) {
        var sessionIds = getUserSessionIds(userId);
        var sessions = sessionIds.stream()
                .map(this::getSession)
                .filter(Objects::nonNull)
                .toList();

        log.debug("Fetched {} sessions for userId={}", sessions.size(), userId);
        return sessions;
    }

    /**
     * Returns the total number of sessions for a given user from the cache.
     *
     * @param userId the user identifier
     * @return the number of active sessions known for the user
     */
    public int totalUserSessions(String userId) {
        return getUserSessionIds(userId).size();
    }

    /**
     * Updates or creates the user-to-session mapping after a session is added.
     *
     * @param userId       the user identifier
     * @param sessionEntry the session to associate
     */
    private void mapNewSessionIdToUser(String userId, VaultiqSessionCacheEntry sessionEntry) {
        var sessionIds = getUserSessionIds(userId);
        sessionIds.add(sessionEntry.getSessionId());

        SessionIds updated = new SessionIds();
        updated.setSessionIds(sessionIds);
        userSessionMappingCache.put(keyForUserSessionMapping(userId), updated);
    }

    /**
     * Returns the cached set of active session IDs for a user, or an empty set if none found.
     *
     * @param userId the user identifier
     * @return set of session IDs (maybe empty)
     */
    public Set<String> getUserSessionIds(String userId) {
        return Optional.ofNullable(userSessionMappingCache.get(keyForUserSessionMapping(userId), SessionIds.class))
                .map(SessionIds::getSessionIds)
                .orElseGet(HashSet::new);
    }

    /**
     * Converts a cache entry into a VaultiqSession model object.
     *
     * @param source cache entry
     * @return VaultiqSession model
     */
    private VaultiqSession toVaultiqSession(VaultiqSessionCacheEntry source) {
        return VaultiqSession.builder()
                .sessionId(source.getSessionId())
                .userId(source.getUserId())
                .deviceFingerPrint(source.getDeviceFingerPrint())
                .createdAt(source.getCreatedAt())
                .isBlocked(source.isBlocked())
                .blockedAt(source.getBlockedAt())
                .build();
    }

    /**
     * Caches multiple sessions and updates the user-to-session mapping in batch.
     *
     * @param userId   the user identifier
     * @param sessions the list of VaultiqSession to cache
     */
    public void cacheUserSessions(String userId, List<VaultiqSession> sessions) {
        sessions.forEach(this::cacheSession);
        var sessionIds = sessions.stream().map(VaultiqSession::getSessionId).collect(Collectors.toSet());
        SessionIds ids = new SessionIds();
        ids.setSessionIds(sessionIds);
        userSessionMappingCache.put(keyForUserSessionMapping(userId), ids);
    }

    /**
     * Deletes all sessions for a given user from the cache and user-session mapping cache.
     *
     * @param sessionIds the session identifiers to delete
     */
    public void deleteAllSessions(Set<String> sessionIds) {
        sessionIds.forEach(this::deleteSession);
        log.debug("Deleted {} sessions via cache. Sessions: {}", sessionIds.size(), sessionIds);
    }

    /**
     * Retrieves all active sessions for a given user, filtering out blocked sessions.
     * @param userId the user identifier
     * @return list of VaultiqSession objects (maybe empty if none)
     */
    public List<VaultiqSession> getActiveSessionsByUser(String userId) {
        return getSessionsByUser(userId)
                .stream()
                .peek(session -> {
                    if (session.isBlocked())
                        deleteSession(session.getSessionId());
                })
                .filter(VaultiqSession::isBlocked)
                .collect(Collectors.toList());
    }
}
