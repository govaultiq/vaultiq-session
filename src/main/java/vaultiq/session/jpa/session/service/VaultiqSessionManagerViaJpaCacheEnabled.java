package vaultiq.session.jpa.session.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vaultiq.session.cache.service.internal.SessionFingerprintCacheService;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.cache.service.internal.VaultiqSessionCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.jpa.session.service.internal.VaultiqSessionEntityService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_AND_CACHE, type = ModelType.SESSION)
public class VaultiqSessionManagerViaJpaCacheEnabled implements VaultiqSessionManager {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpaCacheEnabled.class);

    private final VaultiqSessionEntityService sessionService;
    private final ObjectProvider<VaultiqSessionCacheService> cacheServiceProvider;
    private final SessionFingerprintCacheService fingerprintCacheService;

    /**
     * Constructs a new {@code VaultiqSessionManagerViaJpaCacheEnabled} with the required
     * JPA and Cache session service dependencies.
     *
     * @param sessionService       The underlying service for JPA-based session operations.
     * @param cacheServiceProvider The underlying service for cache-based session operations.
     */
    public VaultiqSessionManagerViaJpaCacheEnabled(
            VaultiqSessionEntityService sessionService,
            ObjectProvider<VaultiqSessionCacheService> cacheServiceProvider,
            SessionFingerprintCacheService fingerprintCacheService
    ) {
        this.sessionService = sessionService;
        this.cacheServiceProvider = cacheServiceProvider;
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
        // Cache session if sessionCacheService is available.
        cacheServiceProvider.ifAvailable(service -> service.cacheSession(session));
        fingerprintCacheService.cacheSessionFingerPrint(session);
        return session;
    }

    /**
     * @inheritDoc <p>
     * Attempts to retrieve the session from the cache first. If not found, it fetches
     * the session from the JPA service and then caches it before returning.
     * </p>
     */
    @Override
    public VaultiqSession getSession(String sessionId) {
        var cacheService = cacheServiceProvider.getIfAvailable();

        // Try cache
        if (cacheService != null) {
            var fromCache = cacheService.getSession(sessionId);
            if (fromCache != null) {
                return fromCache;
            }
            log.debug("Session '{}' not found in cache. Fetching from DB.", sessionId);
        }

        // Fallback to DB
        var session = sessionService.get(sessionId);
        if (session != null && cacheService != null) {
            cacheService.cacheSession(session);
        }
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
            cacheServiceProvider.ifAvailable(service -> service.deleteSession(sessionId));
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
        cacheServiceProvider.ifAvailable(service -> service.deleteAllSessions(sessionIds));
        fingerprintCacheService.evictAllSessions(sessionIds);
    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        return fetchAndCache(
                cacheService -> cacheService.getSessionsByUser(userId),
                sessionService::list,
                (id, list) -> svcCache().cacheUserSessions(id, list),
                "Sessions for user",
                userId
        );
    }

    @Override
    public List<VaultiqSession> getActiveSessionsByUser(String userId) {
        return fetchAndCache(
                cacheService -> cacheService.getActiveSessionsByUser(userId),
                sessionService::getActiveSessionsByUser,
                (id, list) -> svcCache().cacheUserSessions(id, list),
                "Active sessions for user",
                userId
        );
    }

    /**
     * Common read‐through/write‐through helper.
     *
     * @param cacheFetcher   how to fetch from cache given the cache‐service
     * @param dbFetcher      how to fetch from JPA given the userId
     * @param cacheWriter    how to store into cache given (userId, sessions)
     * @param logContext     prefix for your debug log
     * @param userId         the key to look up
     */
    private List<VaultiqSession> fetchAndCache(
            Function<VaultiqSessionCacheService, List<VaultiqSession>> cacheFetcher,
            Function<String, List<VaultiqSession>> dbFetcher,
            BiConsumer<String, List<VaultiqSession>> cacheWriter,
            String logContext,
            String userId) {

        var cacheService = cacheServiceProvider.getIfAvailable();
        if (cacheService != null) {
            var fromCache = cacheFetcher.apply(cacheService);
            if (fromCache != null && !fromCache.isEmpty()) {
                return fromCache;
            }
            log.debug("{} '{}' not found in cache. Fetching from DB.", logContext, userId);
        }

        var fromDb = Optional.ofNullable(dbFetcher.apply(userId))
                .orElseGet(ArrayList::new);

        if (cacheService != null && !fromDb.isEmpty()) {
            cacheWriter.accept(userId, fromDb);
        }
        return fromDb;
    }

    /** helper to avoid repeating provider lookup */
    private VaultiqSessionCacheService svcCache() {
        return cacheServiceProvider.getIfAvailable();
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
        var cacheService = cacheServiceProvider.getIfAvailable();
        if (cacheService != null) {
            Set<String> sessionIds = cacheService.getUserSessionIds(userId);
            if (sessionIds != null && !sessionIds.isEmpty()) {
                log.debug("Counting sessions for user '{}' via cache.", userId);
                return sessionIds.size();
            } else
                log.debug("Session IDs for user '{}' not found in cache. Counting via DB.", userId);
        }
        return sessionService.count(userId);
    }
}
