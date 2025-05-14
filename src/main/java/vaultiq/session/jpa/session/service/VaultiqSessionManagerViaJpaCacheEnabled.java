package vaultiq.session.jpa.session.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.cache.service.internal.VaultiqSessionCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.jpa.session.service.internal.VaultiqSessionEntityService;

import java.util.List;
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
 * for the relevant model types ({@link ModelType#SESSION}, {@link ModelType#USER_SESSION_MAPPING}),
 * as defined by {@link ConditionalOnVaultiqPersistence}.
 * </p>
 *
 * @see VaultiqSessionManager
 * @see VaultiqSessionEntityService
 * @see VaultiqSessionCacheService
 * @see ConditionalOnVaultiqPersistence
 * @see VaultiqPersistenceMode#JPA_AND_CACHE
 * @see ModelType#SESSION
 * @see ModelType#USER_SESSION_MAPPING
 */
@Service
@ConditionalOnBean({VaultiqSessionEntityService.class, VaultiqSessionCacheService.class})
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_AND_CACHE, type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
public class VaultiqSessionManagerViaJpaCacheEnabled implements VaultiqSessionManager {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpaCacheEnabled.class);

    private final VaultiqSessionEntityService sessionService;
    private final VaultiqSessionCacheService cacheService;

    /**
     * Constructs a new {@code VaultiqSessionManagerViaJpaCacheEnabled} with the required
     * JPA and Cache session service dependencies.
     *
     * @param sessionService The underlying service for JPA-based session operations.
     * @param cacheService   The underlying service for cache-based session operations.
     */
    public VaultiqSessionManagerViaJpaCacheEnabled(VaultiqSessionEntityService sessionService,
                                                   VaultiqSessionCacheService cacheService) {
        this.sessionService = sessionService;
        this.cacheService = cacheService;
    }

    /**
     * @inheritDoc
     * <p>
     * Creates the session using the JPA service and then caches the newly created session.
     * </p>
     */
    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        // Create the session in the database via the JPA service.
        VaultiqSession session = sessionService.create(userId, request);
        log.debug("Storing newly created session '{}' in cache.", session.getSessionId());
        // Cache the newly created session.
        cacheService.cacheSession(session);
        return session;
    }

    /**
     * @inheritDoc
     * <p>
     * Attempts to retrieve the session from the cache first. If not found, it fetches
     * the session from the JPA service and then caches it before returning.
     * </p>
     */
    @Override
    public VaultiqSession getSession(String sessionId) {
        // Attempt to get the session from the cache.
        VaultiqSession session = cacheService.getSession(sessionId);
        if (session == null) {
            log.debug("Session '{}' not found in cache. Fetching from DB.", sessionId);
            // If not in cache, fetch from the database via the JPA service.
            session = sessionService.get(sessionId);
            if (session != null) {
                // If found in DB, cache it for future lookups.
                cacheService.cacheSession(session);
            }
        }
        return session;
    }

    /**
     * @inheritDoc
     * <p>
     * Deletes the session from both the JPA store and the cache. It first attempts
     * to retrieve the session to ensure it exists before attempting deletion.
     * </p>
     */
    @Override
    public void deleteSession(String sessionId) {
        log.debug("Deleting session '{}' from DB and cache.", sessionId);
        // Retrieve the session first to confirm existence before deleting.
        VaultiqSession session = getSession(sessionId); // Uses read-through behavior
        if (session != null) {
            // Delete from the database via the JPA service.
            sessionService.delete(sessionId);
            // Delete from the cache.
            cacheService.deleteSession(sessionId);
        } else {
            log.debug("Session '{}' not found while trying to delete.", sessionId);
        }
    }

    /**
     * @inheritDoc
     * <p>
     * Attempts to retrieve all sessions for a user from the cache. If the cache
     * is empty for the user, it fetches the sessions from the JPA service and
     * then caches the list of sessions before returning.
     * </p>
     */
    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        // Attempt to get sessions for the user from the cache.
        var sessions = cacheService.getSessionsByUser(userId);

        if (sessions.isEmpty()) {
            log.debug("Sessions for user '{}' not found in cache. Fetching from DB.", userId);
            // If cache is empty for the user, fetch from the database via the JPA service.
            sessions = sessionService.list(userId);
            // Cache the list of sessions for the user.
            cacheService.cacheUserSessions(userId, sessions);
        }
        return sessions;
    }

    /**
     * @inheritDoc
     * <p>
     * Attempts to get the count of sessions for a user from the cache (by checking
     * the size of the user's session ID set). If the cache does not contain the
     * session IDs for the user, it falls back to counting sessions using the JPA service.
     * </p>
     */
    @Override
    public int totalUserSessions(String userId) {
        // Attempt to get session IDs from the cache to determine the count.
        Set<String> sessionIds = cacheService.getUserSessionIds(userId);
        if (!sessionIds.isEmpty()) {
            log.debug("Counting sessions for user '{}' via cache.", userId);
            return sessionIds.size();
        } else {
            log.debug("Session IDs for user '{}' not found in cache. Counting via DB.", userId);
            // If session IDs are not in cache, fall back to counting in the database.
            return sessionService.count(userId);
        }
    }
}
