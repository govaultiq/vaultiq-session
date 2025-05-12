
package vaultiq.session.cache.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.cache.service.internal.SessionBlocklistCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.SessionBacklistManager;
import vaultiq.session.core.util.VaultiqSessionContext;

import java.util.Arrays;
import java.util.Set;

/**
 * Implementation of {@link SessionBacklistManager} that manages the blocklist state via an in-memory or distributed cache.
 * <p>
 * This implementation is enabled only when Vaultiq is configured for cache-only blocklist management
 * ({@code VaultiqPersistenceMode.CACHE_ONLY} for {@code ModelType.BLOCKLIST}).
 * Delegates all business logic to an injected {@link SessionBlocklistCacheService}.
 *
 * @see SessionBlocklistCacheService
 */
@Service
@ConditionalOnBean(VaultiqSessionContext.class)
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.CACHE_ONLY, type = ModelType.BLOCKLIST)
public class SessionBacklistManagerViaCache implements SessionBacklistManager {
    private static final Logger log = LoggerFactory.getLogger(SessionBacklistManagerViaCache.class);
    private final SessionBlocklistCacheService cacheService;

    /**
     * Constructs the manager with the required cache-based blocklist service.
     *
     * @param cacheService Cache-backed blocklist handler (must not be null)
     */
    public SessionBacklistManagerViaCache(SessionBlocklistCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Blocklist (invalidate) all sessions for a user. Delegates to the cache service.
     *
     * @param userId the user identifier
     */
    @Override
    public void blocklistAllSessions(String userId) {
        log.debug("Blocking all sessions for user: {}", userId);
        cacheService.blocklistAllSessions(userId);
    }

    /**
     * Blocklist all sessions except those explicitly excluded (by session id).
     * Delegates to the cache service.
     *
     * @param userId the user identifier
     * @param excludedSessionIds session ids to leave unblocked
     */
    @Override
    public void blocklistAllSessionsExcept(String userId, String... excludedSessionIds) {
        log.debug("Blocking all sessions except for user: {} and excluded sessions: {}", userId, Arrays.toString(excludedSessionIds));
        cacheService.blocklistAllSessionsExcept(userId, excludedSessionIds);
    }

    /**
     * Blocklist (invalidate) a single session.
     * Delegates to the cache service.
     *
     * @param sessionId session id to blocklist
     */
    @Override
    public void blocklistSession(String sessionId) {
        log.debug("Blocking session: {}", sessionId);
        cacheService.blocklistSession(sessionId);
    }

    /**
     * Determine if the session is present in the blocklist.
     * Delegates to the cache service.
     *
     * @param sessionId session id to check
     * @return true if session is blocklisted
     */
    @Override
    public boolean isSessionBlocklisted(String sessionId) {
        log.debug("Checking if session is blocklisted: {}", sessionId);
        return cacheService.isSessionBlocklisted(sessionId);
    }

    /**
     * Retrieve all blocklisted session ids for a user.
     * Delegates to the cache service.
     *
     * @param userId the user identifier
     * @return a set of blocklisted session ids (never null)
     */
    @Override
    public Set<String> getBlocklistedSessions(String userId) {
        log.debug("Getting blocklisted sessions for user: {}", userId);
        return cacheService.getBlocklistedSessions(userId);
    }
}