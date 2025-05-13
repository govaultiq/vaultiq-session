package vaultiq.session.jpa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.cache.model.SessionBlocklistCacheEntry;
import vaultiq.session.cache.service.internal.SessionBlocklistCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.SessionBacklistManager;
import vaultiq.session.core.contracts.UserIdentityAware;
import vaultiq.session.core.model.SessionBlocklist;
import vaultiq.session.core.util.BlocklistContext;
import vaultiq.session.jpa.model.SessionBlocklistEntity;
import vaultiq.session.jpa.service.internal.SessionBlocklistJpaService;

import java.util.*;


@Service
@ConditionalOnBean({SessionBlocklistJpaService.class, SessionBlocklistCacheService.class})
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_ONLY, type = {ModelType.BLOCKLIST, ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
public class SessionBlocklistManagerViaJpaCacheEnabled implements SessionBacklistManager {
    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistManagerViaJpaCacheEnabled.class);

    private final SessionBlocklistJpaService sessionBlocklistJpaService;
    private final SessionBlocklistCacheService sessionBlocklistCacheService;
    private final UserIdentityAware userIdentityAware;

    public SessionBlocklistManagerViaJpaCacheEnabled(
            SessionBlocklistJpaService sessionBlocklistJpaService,
            SessionBlocklistCacheService sessionBlocklistCacheService,
            UserIdentityAware userIdentityAware
    ) {
        this.sessionBlocklistJpaService = sessionBlocklistJpaService;
        this.sessionBlocklistCacheService = sessionBlocklistCacheService;
        this.userIdentityAware = userIdentityAware;
    }

    @Override
    public void blocklist(BlocklistContext context) {
        log.debug("Blocking sessions with Revocation type/mode: {}", context.getRevocationType().name());
        var entity = sessionBlocklistJpaService.blocklistSession(context.getIdentifier(), context.getNote());
        if(entity != null) cacheEntity(entity);
    }

    @Override
    public boolean isSessionBlocklisted(String sessionId) {
        log.debug("Checking if session '{}' is blocklisted.", sessionId);

        // 1. Check if the session is blocklisted in the cache
        if (sessionBlocklistCacheService.isSessionBlocklisted(sessionId)) {
            return true;
        }

        // 2. If not found in the cache, check the database (JPA)
        if (sessionBlocklistJpaService.isSessionBlocklisted(sessionId)) {
            var entity = sessionBlocklistJpaService.getBlocklistedSession(sessionId);
            return cacheEntity(entity);
        }

        // 3. If not found in cache or db, return false (session is not blocklisted)
        return false;
    }

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

    @Override
    public List<SessionBlocklist> getBlocklistedSessions(String userId) {
        log.debug("Fetching blocklisted sessions for user '{}'.", userId);

        // Step 1: Fetch from cache
        List<SessionBlocklist> cacheBlocklisted = sessionBlocklistCacheService.getAllBlockListByUser(userId)
                .stream()
                .map(this::toSessionBlocklist)
                .toList();

        // Step 2: Compare count from DB only if needed
        long dbCount = sessionBlocklistJpaService.countOfSessionByUser(userId);

        if (cacheBlocklisted.size() >= dbCount) {
            log.debug("Cache is sufficient ({} >= {}). Returning cached data.", cacheBlocklisted.size(), dbCount);
            return cacheBlocklisted;
        }

        // Step 3: Cache might be stale/incomplete → fallback to a merged result
        log.warn("Cache size ({}) less than DB count ({}). Merging with DB.", cacheBlocklisted.size(), dbCount);
        List<SessionBlocklist> dbBlocklisted = sessionBlocklistJpaService.getBlocklistedSessions(userId)
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
     * Helper method to convert a JPA entity to a SessionBlocklist object.
     * <p>
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
     * Helper method to convert a JPA entity to a SessionBlocklist object.
     * <p>
     *
     * @param entry the JPA entity {@link SessionBlocklistEntity} to convert
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

