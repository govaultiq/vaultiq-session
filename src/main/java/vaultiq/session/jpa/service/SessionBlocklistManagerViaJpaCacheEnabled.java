package vaultiq.session.jpa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.cache.service.internal.SessionBlocklistCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.SessionBacklistManager;
import vaultiq.session.jpa.service.internal.SessionBlocklistJpaService;

import java.util.Set;

@Service
@ConditionalOnBean({SessionBlocklistJpaService.class, SessionBlocklistCacheService.class})
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_ONLY, type = {ModelType.BLOCKLIST, ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
public class SessionBlocklistManagerViaJpaCacheEnabled implements SessionBacklistManager {
    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistManagerViaJpaCacheEnabled.class);

    private final SessionBlocklistJpaService sessionBlocklistJpaService;
    private final SessionBlocklistCacheService sessionBlocklistCacheService;

    public SessionBlocklistManagerViaJpaCacheEnabled(
            SessionBlocklistJpaService sessionBlocklistJpaService,
            SessionBlocklistCacheService sessionBlocklistCacheService
    ) {
        this.sessionBlocklistJpaService = sessionBlocklistJpaService;
        this.sessionBlocklistCacheService = sessionBlocklistCacheService;
    }

    @Transactional
    @Override
    public void blocklistAllSessions(String userId) {
        log.debug("Blocklisting all sessions for user '{}'.", userId);
        sessionBlocklistJpaService.blocklistAllSessions(userId);
        var sessionIds = sessionBlocklistJpaService.getBlocklistedSessions(userId);
        sessionBlocklistCacheService.blocklistAllSessions(userId, sessionIds);
    }

    @Override
    public void blocklistAllSessionsExcept(String userId, String... excludedSessionIds) {
        log.debug("Blocklisting all sessions for user '{}' except {}.", userId, excludedSessionIds);
        sessionBlocklistJpaService.blocklistAllSessionsExcept(userId, excludedSessionIds);
        var sessionIds = sessionBlocklistJpaService.getBlocklistedSessions(userId);
        sessionBlocklistCacheService.blocklistAllSessions(userId, sessionIds);
    }

    @Override
    public void blocklistSession(String sessionId) {
        log.debug("Blocklisting session '{}'.", sessionId);
        sessionBlocklistJpaService.blocklistSession(sessionId);
        sessionBlocklistCacheService.blocklistSession(sessionId);
    }

    @Override
    public boolean isSessionBlocklisted(String sessionId) {
        log.debug("Checking if session '{}' is blocklisted.", sessionId);

        /* TODO: Update the VaultiqSession to have blocked status,
         *  1. Check if the session is blocklisted in the cache,
         *  2. If not found in the cache, try getting the VaultiqSession using VaultiqSessionManager
         *  3. If not found take a look if the session is blocklisted in the database
         *  4. If not found return true.(meaning the system is no more aware of this session)
         */
        return sessionBlocklistJpaService.isSessionBlocklisted(sessionId);
    }

    @Override
    public Set<String> getBlocklistedSessions(String userId) {
        log.debug("Fetching blocklisted sessions for user '{}'.", userId);

        /* TODO:need to implement this better,
         *  should have fallback mechanism to check if the blockSessions are anywhere found.
         */
        return sessionBlocklistJpaService.getBlocklistedSessions(userId);
    }
}
