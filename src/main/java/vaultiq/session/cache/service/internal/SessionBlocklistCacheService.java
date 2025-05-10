
package vaultiq.session.cache.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.cache.model.SessionIds;
import vaultiq.session.cache.util.VaultiqCacheContext;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.core.util.VaultiqSessionContext;

import java.util.*;

import static vaultiq.session.cache.util.CacheKeyResolver.*;

/**
 * Service for managing blocklisted (blocklisted) sessions using cache.
 */
@Service
@ConditionalOnBean(VaultiqCacheContext.class)
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.BLOCKLIST)
public class SessionBlocklistCacheService {
    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistCacheService.class);

    private final VaultiqSessionCacheService vaultiqSessionCacheService;
    private final Cache blocklistCache;

    public SessionBlocklistCacheService(
            VaultiqSessionContext context,
            VaultiqCacheContext cacheContext,
            VaultiqSessionCacheService vaultiqSessionCacheService) {
        var blocklistModelConfig = context.getModelConfig(ModelType.BLOCKLIST);
        if (blocklistModelConfig == null) {
            throw new IllegalArgumentException("Blocklist model config not found! Check your configuration for ModelType.BLOCKLIST.");
        }
        this.vaultiqSessionCacheService = Objects.requireNonNull(vaultiqSessionCacheService, "VaultiqSessionCacheService may not be null");
        this.blocklistCache = cacheContext.getCacheMandatory(
                blocklistModelConfig.cacheName(),
                ModelType.BLOCKLIST);
    }

    /**
     * Blocklists (invalidates) a session by its ID.
     *
     * @param sessionId the session to blocklist
     */
    public void blocklistSession(String sessionId) {
        var session = vaultiqSessionCacheService.getSession(sessionId);
        if (session == null) {
            log.info("Attempt to blocklist non-existent session: {}", sessionId);
            return;
        }
        if (isSessionBlocklisted(sessionId)) {
            log.debug("Session with sessionId={} is already blocklisted.", sessionId);
            return;
        }
        blockSession(session.getUserId(), sessionId);
    }

    // Internal helper: blocks a session
    private void blockSession(String userId, String sessionId) {
        blocklistCache.put(keyForBlacklist(sessionId), userId);
        vaultiqSessionCacheService.deleteSession(sessionId);

        SessionIds sessionIds = Optional.ofNullable(getBlocklistByUser(userId)).orElseGet(SessionIds::new);
        sessionIds.addSessionId(sessionId);
        blocklistCache.put(keyForBlacklistByUser(userId), sessionIds);

        log.info("User '{}' had session '{}' blocked.", userId, sessionId);
    }

    // Retrieves all blocklisted session IDs for a user. For extension/testing.
    protected SessionIds getBlocklistByUser(String userId) {
        return blocklistCache.get(keyForBlacklistByUser(userId), SessionIds.class);
    }

    /**
     * Blocklists all sessions for a user.
     *
     * @param userId the user whose sessions should be blocklisted
     */
    public void blocklistAllSessions(String userId) {
        var sessionIds = vaultiqSessionCacheService.getUserSessionIds(userId);
        if (sessionIds.isEmpty()) {
            log.info("No sessions to blocklist for user '{}'.", userId);
        }
        sessionIds.forEach(id -> blockSession(userId, id));
    }

    /**
     * Blocklists all sessions except the specified session IDs (e.g. keep current device logged in).
     *
     * @param userId the user whose sessions should be blocklisted
     * @param excludedSessionIds session IDs that should NOT be blocklisted
     */
    public void blocklistAllSessionsExcept(String userId, String... excludedSessionIds) {
        Set<String> excludedSet = (excludedSessionIds == null) ? Collections.emptySet() : Set.of(excludedSessionIds);
        var sessionIds = vaultiqSessionCacheService.getUserSessionIds(userId)
                .stream().filter(id -> !excludedSet.contains(id)).toList();
        if (sessionIds.isEmpty()) {
            log.info("No sessions to blocklist for user '{}', all are excluded.", userId);
        }
        sessionIds.forEach(id -> blockSession(userId, id));
    }

    /**
     * Checks if a session ID is blocklisted.
     *
     * @param sessionId the session to check
     * @return true if blocklisted
     */
    public boolean isSessionBlocklisted(String sessionId) {
        return blocklistCache.get(keyForBlacklist(sessionId)) != null;
    }

    /**
     * Gets all blocklisted session IDs for a given user.
     *
     * @param userId for which blocklisted session IDs are returned
     * @return set of blocklisted session IDs
     */
    public Set<String> getBlocklistedSessions(String userId) {
        return Optional.ofNullable(getBlocklistByUser(userId))
                .orElseGet(SessionIds::new)
                .getSessionIds();
    }

    /**
     * Unblock (remove from blocklist) a session by its session id, if required in the future.
     */
     public void unblockSession(String sessionId) {
         blocklistCache.evict(keyForBlacklist(sessionId));
         // Consider also removing from user's SessionIds collection.
         log.info("Session with sessionId={} unblocked.", sessionId);
     }
}
