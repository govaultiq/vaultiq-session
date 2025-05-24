package vaultiq.session.jpa.session.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vaultiq.session.cache.model.SessionIds;
import vaultiq.session.cache.service.internal.SessionFingerprintCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistenceRequirement;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.cache.service.internal.VaultiqSessionCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.jpa.session.service.internal.VaultiqSessionEntityService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of the {@link VaultiqSessionManager} interface that utilizes
 * both JPA (database) and Cache persistence for managing Vaultiq sessions.
 * <p>
 * This service implements a read-through/write-through caching strategy for
 * session data. Read operations ({@link #getSession(String)}, {@link #getSessionsByUser(String)},
 * {@link #totalUserSessions(String)}) prioritize fetching data from the cache
 * and fall back to the JPA service if the data is not found in the cache.
 * Write operations ({@link #createSession(String, HttpServletRequest)}, {@link #deleteSession(String)})
 * update both the JPA store and the cache to maintain consistency.
 * </p>
 * <p>
 * This bean is automatically configured by Spring when both a {@link VaultiqSessionEntityService}
 * (for JPA operations) and a {@link VaultiqSessionCacheService} (for cache operations)
 * are available, and the persistence configuration matches {@link VaultiqPersistenceMode#JPA_AND_CACHE}
 * for the relevant model type {@link ModelType#SESSION},
 * as defined by {@link ConditionalOnVaultiqPersistence}.
 * </p>
 *
 * @see VaultiqSessionManager
 * @see VaultiqSessionEntityService
 * @see VaultiqSessionCacheService
 * @see ConditionalOnVaultiqPersistence
 * @see VaultiqPersistenceMode#JPA_AND_CACHE
 * @see ModelType#SESSION
 */
@Service
@ConditionalOnVaultiqPersistenceRequirement(VaultiqPersistenceMethod.USE_CACHE)
public class VaultiqSessionManagerViaJpaCacheEnabled implements VaultiqSessionManager {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpaCacheEnabled.class);

    private final VaultiqSessionEntityService sessionService;
    private final VaultiqSessionCacheService sessionCacheService;
    private final SessionFingerprintCacheService fingerprintCacheService;

    /**
     * Constructs a new {@code VaultiqSessionManagerViaJpaCacheEnabled} with the required
     * JPA and Cache session service dependencies.
     *
     * @param sessionService      The underlying service for JPA-based session operations.
     * @param sessionCacheService The underlying service for cache-based session operations.
     */
    public VaultiqSessionManagerViaJpaCacheEnabled(
            VaultiqSessionEntityService sessionService,
            VaultiqSessionCacheService sessionCacheService,
            SessionFingerprintCacheService fingerprintCacheService
    ) {
        this.sessionService = sessionService;
        this.sessionCacheService = sessionCacheService;
        this.fingerprintCacheService = fingerprintCacheService;
        log.info("VaultiqSessionManager initialized; Persistence via - JPA_AND_CACHE.");
    }

    /**
     * @inheritDoc <p>
     * Creates the session using the JPA service and then caches the newly created session.
     * </p>
     */
    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        // Create the session in the database via the JPA service.
        VaultiqSession session = sessionService.create(userId, request);
        log.debug("Storing newly created session '{}' in cache.", session.getSessionId());

        sessionCacheService.cacheSession(session);
        fingerprintCacheService.cacheSessionFingerPrint(session);

        return session;
    }

    /**
     * @inheritDoc <p>
     * Deletes the session from both the JPA store and the cache. It first attempts
     * to retrieve the session to ensure it exists before attempting deletion.
     * </p>
     */
    @Override
    public void deleteSession(String sessionId) {
        log.debug("Deleting session '{}' from DB and cache.", sessionId);
        VaultiqSession session = sessionService.get(sessionId);
        if (session != null) {
            sessionService.delete(sessionId);
            sessionCacheService.deleteSession(sessionId);
            fingerprintCacheService.evictSessionFingerPrint(sessionId);
        } else {
            log.debug("Session '{}' not found while trying to delete.", sessionId);
        }
    }

    /**
     * @param sessionIds A set of unique session IDs to delete.
     * @inheritDoc Deletes all sessions from both the JPA store and the cache.
     */
    @Transactional
    @Override
    public void deleteAllSessions(Set<String> sessionIds) {
        log.debug("Attempting to delete all sessions from both Jpa Store and Cache.");
        sessionService.deleteAllSessions(sessionIds);
        sessionCacheService.deleteAllSessions(sessionIds);
        fingerprintCacheService.evictAllSessions(sessionIds);
    }

    /**
     * @inheritDoc <p>
     * Attempts to retrieve the session from the cache first. If not found, it fetches
     * the session from the JPA service and then caches it before returning.
     * </p>
     */
    @Override
    public VaultiqSession getSession(String sessionId) {
        var session = sessionCacheService.getSession(sessionId);

        if (session == null) {
            log.debug("Session '{}' not found in cache. Fetching from DB.", sessionId);
            session = sessionService.get(sessionId);
            sessionCacheService.cacheSession(session);
        }
        return session;
    }

    /**
     * @param sessionId The unique identifier of the session.
     * @return Optional String (Device fingerprint) if found or empty.
     * @inheritDoc <p>
     * Retrieves all active sessions for a user.
     * </p>
     */
    @Override
    public Optional<String> getSessionFingerprint(String sessionId) {
        return Optional.ofNullable(sessionId)
                .map(fingerprintCacheService::getSessionFingerPrint)
                .or(() -> sessionService.getSessionFingerprint(sessionId));
    }

    /**
     * @param userId The unique identifier of the user whose sessions are to be retrieved.
     * @return
     * @inheritDoc <p>
     * Retrieves all sessions for a user irrespective of its revoked status.
     * </p>
     */
    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        var sessions = sessionCacheService.getSessionsByUser(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No sessions found for user '{}'.", userId);
            sessions = sessionService.list(userId);
            sessionCacheService.cacheUserSessions(userId, sessions);
        }
        return sessions;
    }

    /**
     * @param userId The unique identifier of the user whose sessions are to be retrieved.
     * @return
     * @inheritDoc <p>
     * Retrieves all active sessions for a user.
     * </p>
     */
    @Override
    public List<VaultiqSession> getActiveSessionsByUser(String userId) {
        var sessions = sessionCacheService.getActiveSessionsByUser(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No active sessions found for user '{}'.", userId);
            sessions = sessionService.getActiveSessionsByUser(userId);
            sessionCacheService.cacheUserSessions(userId, sessions);
        }
        return sessions;
    }

    /**
     * @inheritDoc <p>
     * Attempts to get the count of sessions for a user from the cache (by checking
     * the size of the user's session ID set). If the cache does not contain the
     * session IDs for the user, it falls back to counting sessions using the JPA service.
     * </p>
     */
    @Override
    public int totalUserSessions(String userId) {
        Set<String> sessionIds = sessionCacheService.getUserSessionIds(userId)
                .map(SessionIds::getSessionIds).orElse(null);

        if (sessionIds != null && !sessionIds.isEmpty()) {
            log.debug("Counting sessions for user '{}' via cache.", userId);
            return sessionIds.size();
        } else
            log.debug("Session IDs for user '{}' not found in cache. Counting via DB.", userId);

        return sessionService.count(userId);
    }
}
