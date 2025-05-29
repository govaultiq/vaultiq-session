
package vaultiq.session.cache.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.service.internal.SessionFingerprintCacheService;
import vaultiq.session.core.SessionManager;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.cache.service.internal.VaultiqSessionCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.model.VaultiqSession;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Cache-only implementation of {@link SessionManager} for Vaultiq sessions.
 * <p>
 * Delegates all session pool operations (creation, retrieval, deletion, listing, and counting) to the internal
 * {@link VaultiqSessionCacheService}. Used as the runtime manager when the system is configured for cache-only mode.
 * </p>
 * <ul>
 *   <li>Enabled by {@link VaultiqPersistenceMode#CACHE_ONLY} for session and user-session-mapping model types.</li>
 *   <li>Injects the cache-backed service and wraps its functionality with the standard interface.</li>
 * </ul>
 */
@Service
@ConditionalOnBean(VaultiqSessionCacheService.class)
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.CACHE_ONLY, type = ModelType.SESSION)
public class SessionManagerViaCache implements SessionManager {

    private final static Logger log = LoggerFactory.getLogger(SessionManagerViaCache.class);
    private final VaultiqSessionCacheService vaultiqSessionCacheService;
    private final SessionFingerprintCacheService sessionFingerprintCacheService;

    /**
     * Instantiates the manager with its required cache-backed service dependency.
     *
     * @param vaultiqSessionCacheService the service providing cache-based CRUD for sessions
     */
    public SessionManagerViaCache(VaultiqSessionCacheService vaultiqSessionCacheService, SessionFingerprintCacheService sessionFingerprintCacheService) {
        this.vaultiqSessionCacheService = vaultiqSessionCacheService;
        this.sessionFingerprintCacheService = sessionFingerprintCacheService;
    }

    /**
     * Creates and caches a new session for a user.
     *
     * @param userId  the user identifier
     * @param request HTTP request for device fingerprint extraction
     * @return the created session
     */
    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        // both of these Services use CacheHelper, who fail silently if Cache doesn't exist.
        var session = vaultiqSessionCacheService.createSession(userId, request);
        sessionFingerprintCacheService.cacheSessionFingerPrint(session);
        return session;
    }

    /**
     * Retrieves a session by sessionId from the cache.
     *
     * @param sessionId the session identifier
     * @return the session if found; otherwise null
     */
    @Override
    public VaultiqSession getSession(String sessionId) {
        return vaultiqSessionCacheService.getSession(sessionId);
    }

    /**
     * Retrieves the device fingerprint associated with a session by sessionId.
     * @param sessionId The unique identifier of the session.
     * @return Optional instance of the device fingerprint, or empty if not found.
     */
    @Override
    public Optional<String> getSessionFingerprint(String sessionId) {
        return Optional.ofNullable(sessionFingerprintCacheService.getSessionFingerPrint(sessionId));
    }

    /**
     * Deletes a session by sessionId from the cache and mapping.
     *
     * @param sessionId the session identifier to delete
     */
    @Override
    public void deleteSession(String sessionId) {
        // both of these Services use CacheHelper, who fail silently if Cache doesn't exist.
        vaultiqSessionCacheService.deleteSession(sessionId);
        sessionFingerprintCacheService.evictSessionFingerPrint(sessionId);
    }

    /**
     * Deletes all sessions by sessionId from the cache and mapping.
     *
     * @param sessionIds A set of unique session IDs to delete.
     */
    @Override
    public void deleteAllSessions(Set<String> sessionIds) {
        log.debug("Attempting to delete list of sessions via cache.");

        // both of these Services use CacheHelper, who fail silently if Cache doesn't exist.
        vaultiqSessionCacheService.deleteAllSessions(sessionIds);
        sessionFingerprintCacheService.evictAllSessions(sessionIds);
    }

    /**
     * Lists all current sessions for the specified user from the cache.
     *
     * @param userId the user identifier
     * @return the list of sessions may be empty if none
     */
    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        return vaultiqSessionCacheService.getSessionsByUser(userId);
    }

    /**
     * Retrieves all active sessions for the specified user from the cache.
     *
     * @param userId The unique identifier of the user whose sessions are to be retrieved.
     * @return the list of sessions may be empty if none
     */
    @Override
    public List<VaultiqSession> getActiveSessionsByUser(String userId) {
        return vaultiqSessionCacheService.getActiveSessionsByUser(userId);
    }

    /**
     * Returns a count of all current sessions for the specified user from the cache.
     *
     * @param userId the user identifier
     * @return number of sessions for user
     */
    @Override
    public int totalUserSessions(String userId) {
        return vaultiqSessionCacheService.totalUserSessions(userId);
    }
}
