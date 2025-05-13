package vaultiq.session.cache.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.cache.model.SessionBlocklistCacheEntry;
import vaultiq.session.cache.model.SessionIds;
import vaultiq.session.cache.util.VaultiqCacheContext;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.core.contracts.UserIdentityAware;
import vaultiq.session.core.model.RevocationType;
import vaultiq.session.core.util.BlocklistContext;
import vaultiq.session.core.util.VaultiqSessionContext;

import java.util.*;
import java.util.stream.Collectors;

import static vaultiq.session.cache.util.CacheKeyResolver.*;

/**
 * Service for managing blocklisted sessions in the cache layer.
 */
@Service
@ConditionalOnBean(VaultiqCacheContext.class)
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.BLOCKLIST)
public class SessionBlocklistCacheService {

    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistCacheService.class);

    private final VaultiqSessionCacheService vaultiqSessionCacheService;
    private final Cache blocklistCache;
    private final UserIdentityAware userIdentityAware;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public SessionBlocklistCacheService(
            VaultiqSessionContext context,
            VaultiqCacheContext cacheContext,
            VaultiqSessionCacheService vaultiqSessionCacheService,
            UserIdentityAware userIdentityAware
    ) {
        this.userIdentityAware = userIdentityAware;
        var blocklistModelConfig = context.getModelConfig(ModelType.BLOCKLIST);
        if (blocklistModelConfig == null) {
            throw new IllegalArgumentException("Blocklist model config not found! Check your configuration for ModelType.BLOCKLIST.");
        }

        this.vaultiqSessionCacheService = Objects.requireNonNull(vaultiqSessionCacheService, "VaultiqSessionCacheService may not be null");
        this.blocklistCache = cacheContext.getCacheMandatory(
                blocklistModelConfig.cacheName(),
                ModelType.BLOCKLIST
        );
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Dispatch method for blocklisting based on revocation strategy.
     */
    public void blocklist(BlocklistContext context) {
        if (context != null) {
            switch (context.getRevocationType()) {
                case LOGOUT -> blocklistSession(context.getIdentifier(), context.getNote());
                case LOGOUT_ALL -> blocklistAllSessions(context.getIdentifier(), context.getNote());
                case LOGOUT_WITH_EXCLUSION -> blocklistWithExclusions(
                        context.getIdentifier(),
                        context.getNote(),
                        context.getExcludedSessionIds()
                );
            }
        }
    }

    /**
     * Blocklist a single session.
     *
     * @param entry the session entry to blocklist
     */
    public void blocklist(SessionBlocklistCacheEntry entry) {
        blockSession(entry);
    }

    /**
     * Blocklist a single session.
     */
    public void blocklistSession(String sessionId, String note) {
        var session = vaultiqSessionCacheService.getSession(sessionId);

        if (session == null) {
            log.info("Attempt to blocklist non-existent session: {}", sessionId);
            return;
        }

        if (isSessionBlocklisted(sessionId)) {
            log.debug("Session with sessionId={} is already blocklisted.", sessionId);
            return;
        }

        var entry = SessionBlocklistCacheEntry.create(
                sessionId,
                session.getUserId(),
                RevocationType.LOGOUT,
                note,
                userIdentityAware.getCurrentUserID()
        );

        blockSession(entry);
    }

    /**
     * Blocklist all sessions for a given user.
     */
    public void blocklistAllSessions(String userId, String note) {
        var sessionIds = vaultiqSessionCacheService.getUserSessionIds(userId);
        blocklistSessions(userId, sessionIds, note);
    }

    /**
     * Blocklist all sessions *except* the specified session IDs.
     */
    public void blocklistWithExclusions(String userId, String note, String... excludedSessionIds) {
        Set<String> excludedSet = (excludedSessionIds == null)
                ? Collections.emptySet()
                : Set.of(excludedSessionIds);

        var sessionIds = vaultiqSessionCacheService.getUserSessionIds(userId).stream()
                .filter(id -> !excludedSet.contains(id))
                .collect(Collectors.toSet());

        if (sessionIds.isEmpty()) {
            log.info("No sessions to blocklist for user '{}'; all are excluded.", userId);
        }

        blocklistSessions(userId, sessionIds, note);
    }

    /**
     * Check if a session is blocklisted.
     */
    public boolean isSessionBlocklisted(String sessionId) {
        return blocklistCache.get(keyForBlacklist(sessionId)) != null;
    }

    /**
     * Retrieve blocklisted session IDs for a user.
     */
    public SessionIds getBlocklistByUser(String userId) {
        return blocklistCache.get(keyForBlacklistByUser(userId), SessionIds.class);
    }

    /**
     * Retrieve all blocklisted session entries for a user.
     */
    public List<SessionBlocklistCacheEntry> getAllBlockListByUser(String userId) {
        var sessionIds = getBlocklistByUser(userId);
        if (sessionIds == null || sessionIds.getSessionIds() == null) {
            return List.of();
        }

        return sessionIds.getSessionIds().stream()
                .map(id -> blocklistCache.get(keyForBlacklist(id), SessionBlocklistCacheEntry.class))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Unblock a session from the blocklist.
     */
    public void unblockSession(String sessionId) {
        blocklistCache.evict(keyForBlacklist(sessionId));
        log.info("Session with sessionId={} unblocked.", sessionId);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /**
     * Blocklist specific set of session IDs for a user.
     */
    private void blocklistSessions(String userId, Set<String> sessionIds, String note) {
        if (sessionIds.isEmpty()) {
            log.info("No sessions to blocklist for user '{}'.", userId);
        }

        var sessions = getAllBlockListByUser(userId);

        sessions.forEach(session -> {
            var entry = SessionBlocklistCacheEntry.create(
                    session.getSessionId(),
                    session.getUserId(),
                    RevocationType.LOGOUT,
                    note,
                    userIdentityAware.getCurrentUserID()
            );
            blockSession(entry);
        });
    }

    /**
     * Helper to blocklist a session and update relevant mappings.
     */
    private void blockSession(SessionBlocklistCacheEntry entry) {
        var userId = entry.getUserId();
        var sessionId = entry.getSessionId();

        blocklistCache.put(keyForBlacklist(sessionId), entry);
        vaultiqSessionCacheService.deleteSession(sessionId);

        SessionIds sessionIds = Optional.ofNullable(getBlocklistByUser(userId))
                .orElseGet(SessionIds::new);
        sessionIds.addSessionId(sessionId);
        blocklistCache.put(keyForBlacklistByUser(userId), sessionIds);

        log.info("User '{}' had session '{}' blocked.", userId, sessionId);
    }
}
