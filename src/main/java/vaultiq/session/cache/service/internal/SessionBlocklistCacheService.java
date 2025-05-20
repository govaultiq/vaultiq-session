
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
import vaultiq.session.context.BlocklistContext;
import vaultiq.session.context.VaultiqSessionContext;

import java.util.*;
import java.util.stream.Collectors;

import static vaultiq.session.cache.util.CacheKeyResolver.*;

/**
 * Service for managing blocklisted (invalidated) sessions in the in-memory cache layer.
 * <p>
 * Provides fast lookups and blocklist operations for sessions, with support for blocklisting single or multiple sessions,
 * blocklisting all sessions except some, and querying for a user's blocklisted sessions.
 * </p>
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

    /**
     * Constructs a cache-backed blocklist service for sessions.
     *
     * @param context                    session context/model config accessor
     * @param cacheContext               context holding/initializing cache instances
     * @param vaultiqSessionCacheService backing service for session cache access
     * @param userIdentityAware          used for audit context (marking who triggers blocklists)
     */
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
     * Dispatches a blocklist operation according to the revocation strategy in the provided context.
     *
     * @param context describes what to blocklist and how
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
     * Blocklists a single session by storing an entry in cache.
     *
     * @param entry the session entry to blocklist
     */
    public void blocklist(SessionBlocklistCacheEntry entry) {
        blockSession(entry);
        vaultiqSessionCacheService.deleteSession(entry.getSessionId());
        log.info("Session with sessionId={} blocked.", entry.getSessionId());
    }

    /**
     * Blocklists a single session by sessionId and note.
     *
     * @param sessionId the session's identifier
     * @param note      the note/reason for blocklisting
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
     * Blocklists all sessions for a given user.
     *
     * @param userId the user whose sessions should be blocklisted
     * @param note   note/reason for blocklisting
     */
    public void blocklistAllSessions(String userId, String note) {
        var sessionIds = vaultiqSessionCacheService.getUserSessionIds(userId);
        blocklistSessions(userId, sessionIds, note);
    }

    /**
     * Blocklists all sessions for a user except those specified as excluded.
     *
     * @param userId             the user whose sessions to blocklist
     * @param note               note/reason for blocklisting
     * @param excludedSessionIds session IDs to be excluded from blocklisting
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
     * Checks if a session is currently blocklisted in the cache.
     *
     * @param sessionId the session identifier
     * @return true if blocklisted, false otherwise
     */
    public boolean isSessionBlocklisted(String sessionId) {
        return blocklistCache.get(keyForBlacklist(sessionId)) != null;
    }

    /**
     * Retrieves the blocklisted session IDs for a user.
     *
     * @param userId the user whose blocklisted session IDs to fetch
     * @return a SessionIds object containing IDs and cache update timestamp
     */
    public SessionIds getBlocklistByUser(String userId) {
        return blocklistCache.get(keyForBlacklistByUser(userId), SessionIds.class);
    }

    /**
     * Retrieves all blocklisted session entries for a user from the cache.
     *
     * @param userId the user's identifier
     * @return a list of SessionBlocklistCacheEntry for the user
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
     * Gets the last updated timestamp for the blocklist of a given user.
     *
     * @param userId the user for whom to fetch the last updated time
     * @return an Optional containing the last updated timestamp if present
     */
    public Optional<Long> getLastUpdatedAt(String userId) {
        var sessionIds = getBlocklistByUser(userId);
        return Optional.ofNullable(sessionIds).map(SessionIds::getLastUpdated);
    }

    /**
     * Removes (unblocks) the blocklist entry for a session from the cache.
     *
     * @param sessionId the session identifier to unblock
     */
    public void unblockSession(String sessionId) {
        blocklistCache.evict(keyForBlacklist(sessionId));
        log.info("Session with sessionId={} unblocked.", sessionId);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /**
     * Blocklists a specific set of session IDs for a user.
     *
     * @param userId     the user's identifier
     * @param sessionIds the set of session IDs to blocklist
     * @param note       note/reason for blocklisting
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
     * Helper to add a blocklisted session entry to the cache and remove the session from the session cache.
     * Also updates the mapping of blocklisted sessions for the user.
     *
     * @param entry the blocklist entry to persist
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

    /**
     * Clears the blocklist for a list of session IDs.
     *
     * @param sessionIds the session IDs to clear from the blocklist
     */
    public void clearBlocklist(String... sessionIds) {
        if (sessionIds == null || sessionIds.length == 0) {
            log.info("No sessions to clear from blocklist.");
            return;
        }
        for (String sessionId : sessionIds) {
            blocklistCache.evict(keyForBlacklist(sessionId));
        }
        log.info("Blocklist cleared for {} sessions: {}", sessionIds.length, Arrays.toString(sessionIds));
    }
}
