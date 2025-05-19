
package vaultiq.session.cache.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.cache.model.SessionBlocklistCacheEntry;
import vaultiq.session.cache.service.internal.SessionBlocklistCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.SessionBlocklistManager;
import vaultiq.session.core.model.SessionBlocklist;
import vaultiq.session.core.util.BlocklistContext;
import vaultiq.session.core.util.VaultiqSessionContext;

import java.util.List;

/**
 * Implementation of {@link SessionBlocklistManager} that manages the blocklist state via an in-memory or distributed cache.
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
public class SessionBlocklistManagerViaCache implements SessionBlocklistManager {
    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistManagerViaCache.class);
    private final SessionBlocklistCacheService cacheService;

    /**
     * Constructs the manager with the required cache-based blocklist service.
     *
     * @param cacheService Cache-backed blocklist handler (must not be null)
     */
    public SessionBlocklistManagerViaCache(SessionBlocklistCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Blocklist sessions based on the provided context.
     *
     * @param context the context describing the blocklist operation
     */
    @Override
    public void blocklist(BlocklistContext context) {
        log.debug("Blocking sessions with Revocation type/mode: {}", context.getRevocationType().name());
        cacheService.blocklist(context);
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
     * @return list of blocklisted sessions (never null)
     */
    @Override
    public List<SessionBlocklist> getBlocklistedSessions(String userId) {
        log.debug("Getting blocklisted sessions for user: {}", userId);

        return cacheService.getAllBlockListByUser(userId)
                .stream()
                .map(this::toSessionBlocklist)
                .toList();
    }

    /**
     * Clear the blocklist for a specific session or multiple sessions.
     * Delegates to the cache service.
     *
     * @param sessionIds an array of unique sessions identifiers to clear. Can be empty. It Can be blank.
     */
    @Override
    public void clearBlocklist(String... sessionIds) {
        log.debug("Attempting to clear blocklist for {} sessions", sessionIds.length);
        cacheService.clearBlocklist(sessionIds);
    }

    /**
     * Helper method to convert a cache entry to a SessionBlocklist object.
     *
     * @param entry the cache entry to convert
     * @return the converted SessionBlocklist object
     */
    private SessionBlocklist toSessionBlocklist(SessionBlocklistCacheEntry entry) {
        return SessionBlocklist.builder()
                .sessionId(entry.getSessionId())
                .userId(entry.getUserId())
                .revocationType(entry.getRevocationType())
                .note(entry.getNote())
                .triggeredBy(entry.getTriggeredBy())
                .blocklistedAt(entry.getBlocklistedAt())
                .build();
    }
}