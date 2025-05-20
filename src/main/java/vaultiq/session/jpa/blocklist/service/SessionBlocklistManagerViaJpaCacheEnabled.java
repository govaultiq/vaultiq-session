
package vaultiq.session.jpa.blocklist.service;

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
import vaultiq.session.core.contracts.UserIdentityAware;
import vaultiq.session.core.model.SessionBlocklist;
import vaultiq.session.context.BlocklistContext;
import vaultiq.session.jpa.blocklist.model.SessionBlocklistEntity;
import vaultiq.session.jpa.blocklist.service.internal.SessionBlocklistEntityService;

import java.util.*;

/**
 * An implementation of the SessionBlocklistManager that utilizes both cache and JPA persistence for session blocklisting.
 * <p>
 * Primary preference is given to the cache for retrieving or updating blocklist status to achieve maximum performance;
 * database is used as a fallback and authoritative source to keep the cache updated in case of cache misses or stale data.
 * </p>
 *
 * <p>
 * Typical blocklist/write operations update the database and then populate the cache. Read/check operations first attempt from
 * the cache, falling back to JPA when necessary, and update the cache if needed.
 * </p>
 */
@Service
@ConditionalOnBean({SessionBlocklistEntityService.class, SessionBlocklistCacheService.class})
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_ONLY, type = {ModelType.BLOCKLIST, ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
public class SessionBlocklistManagerViaJpaCacheEnabled implements SessionBlocklistManager {
    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistManagerViaJpaCacheEnabled.class);

    private final SessionBlocklistEntityService sessionBlocklistEntityService;
    private final SessionBlocklistCacheService sessionBlocklistCacheService;
    private final UserIdentityAware userIdentityAware;

    /**
     * Constructs a new SessionBlocklistManagerViaJpaCacheEnabled.
     *
     * @param sessionBlocklistEntityService the service for JPA-based blocklist persistence
     * @param sessionBlocklistCacheService  the service for cache-based blocklist management
     * @param userIdentityAware             used to acquire the current user for audit purposes
     */
    public SessionBlocklistManagerViaJpaCacheEnabled(
            SessionBlocklistEntityService sessionBlocklistEntityService,
            SessionBlocklistCacheService sessionBlocklistCacheService,
            UserIdentityAware userIdentityAware
    ) {
        this.sessionBlocklistEntityService = sessionBlocklistEntityService;
        this.sessionBlocklistCacheService = sessionBlocklistCacheService;
        this.userIdentityAware = userIdentityAware;
    }

    /**
     * Marks sessions as blocklisted according to the provided context.
     * Updates the database and, on successful update, the cache is also refreshed.
     *
     * @param context the blocklist operation context
     */
    @Override
    public void blocklist(BlocklistContext context) {
        log.debug("Blocking sessions with Revocation type/mode: {}", context.getRevocationType().name());
        var entity = sessionBlocklistEntityService.blocklistSession(context.getIdentifier(), context.getNote());
        if (entity != null) cacheEntity(entity);
    }

    /**
     * Checks if a given session is blocklisted, preferring a cache lookup then falling back to JPA if needed.
     * If blocklisted in DB but missing in cache, the cache will be updated.
     *
     * @param sessionId the session identifier to check
     * @return true if the session is blocklisted, false otherwise
     */
    @Override
    public boolean isSessionBlocklisted(String sessionId) {
        log.debug("Checking if session '{}' is blocklisted.", sessionId);

        // 1. Check if the session is blocklisted in the cache
        if (sessionBlocklistCacheService.isSessionBlocklisted(sessionId)) {
            return true;
        }

        // 2. If not found in the cache, check the database (JPA)
        if (sessionBlocklistEntityService.isSessionBlocklisted(sessionId)) {
            var entity = sessionBlocklistEntityService.getBlocklistedSession(sessionId);
            return cacheEntity(entity);
        }

        // 3. If not found in cache or db, return false (session is not blocklisted)
        return false;
    }

    /**
     * Helper method to cache a SessionBlocklistEntity as a SessionBlocklistCacheEntry.
     *
     * @param entity the JPA entity to cache
     * @return always true (for integration as a predicate)
     */
    private boolean cacheEntity(SessionBlocklistEntity entity) {
        var newEntry = new SessionBlocklistCacheEntry();
        newEntry.setSessionId(entity.getSessionId());
        newEntry.setUserId(entity.getUserId());
        newEntry.setRevocationType(entity.getRevocationType());
        newEntry.setNote(entity.getNote());
        newEntry.setTriggeredBy(userIdentityAware.getCurrentUserID());
        newEntry.setBlocklistedAt(entity.getBlocklistedAt());

        sessionBlocklistCacheService.blocklist(newEntry);

        return true;
    }

    /**
     * Retrieves all blocklisted sessions for a given user, checking cache first then merging with the database if the cache is stale.
     *
     * @param userId the user identifier whose blocklisted sessions are to be obtained
     * @return list of blocklisted sessions
     */
    @Override
    public List<SessionBlocklist> getBlocklistedSessions(String userId) {
        log.debug("Fetching blocklisted sessions for user '{}'.", userId);

        // Step 1: Fetch from cache
        List<SessionBlocklist> cacheBlocklisted = sessionBlocklistCacheService.getAllBlockListByUser(userId)
                .stream()
                .map(this::toSessionBlocklist)
                .toList();

        // Step 2: Check if the cache is stale
        var isStale = sessionBlocklistCacheService.getLastUpdatedAt(userId)
                .map(lastUpdatedAt -> sessionBlocklistEntityService.isLastUpdatedGreaterThan(userId, lastUpdatedAt))
                .orElse(false);

        if (!isStale) {
            log.debug("Blocklist found from cache. Not stale.");
            return cacheBlocklisted;
        }

        // Step 3: Cache might be stale/incomplete → fallback to a merged result
        log.debug("Blocklist is stale in cache, fetching from DB.");
        List<SessionBlocklist> dbBlocklisted = sessionBlocklistEntityService.getBlocklistedSessions(userId)
                .stream()
                .map(this::toSessionBlocklist)
                .toList();

        // Step 4: Merge, preferring cache
        Map<String, SessionBlocklist> merged = new LinkedHashMap<>();
        for (SessionBlocklist bl : dbBlocklisted) merged.put(bl.getSessionId(), bl);
        for (SessionBlocklist bl : cacheBlocklisted) merged.put(bl.getSessionId(), bl); // override with cache

        return new ArrayList<>(merged.values());
    }

    /**
     * Clears the blocklist for a specific session or multiple sessions.
     * <p>
     *
     * @param sessionIds an array of unique sessions identifiers to clear. Can be empty. It Can be blank.
     */
    @Override
    public void clearBlocklist(String... sessionIds) {
        log.debug("Attempting to clear blocklist for {} sessions.", sessionIds.length);
        sessionBlocklistEntityService.clearBlocklist(sessionIds);
        sessionBlocklistCacheService.clearBlocklist(sessionIds);
    }

    /**
     * Helper method to convert a JPA entity to a SessionBlocklist object.
     *
     * @param entity the JPA entity {@link SessionBlocklistEntity} to convert
     * @return the converted {@link SessionBlocklist} object
     */
    private SessionBlocklist toSessionBlocklist(SessionBlocklistEntity entity) {
        return SessionBlocklist.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .revocationType(entity.getRevocationType())
                .note(entity.getNote())
                .triggeredBy(entity.getTriggeredBy())
                .blocklistedAt(entity.getBlocklistedAt())
                .build();
    }

    /**
     * Helper method to convert a cache entry to a SessionBlocklist object.
     *
     * @param entry the cache entity {@link SessionBlocklistCacheEntry} to convert
     * @return the converted {@link SessionBlocklist} object
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

