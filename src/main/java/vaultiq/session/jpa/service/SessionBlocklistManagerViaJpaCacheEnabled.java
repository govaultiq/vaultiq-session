package vaultiq.session.jpa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vaultiq.session.cache.service.internal.SessionBlocklistCacheService;
import vaultiq.session.core.SessionBacklistManager;
import vaultiq.session.jpa.service.internal.SessionBlocklistJpaService;

import java.util.Set;

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

    @Override
    public void blocklistAllSessions(String userId) {

    }

    @Override
    public void blocklistAllSessionsExcept(String userId, String... excludedSessionIds) {

    }

    @Override
    public void blocklistSession(String sessionId) {

    }

    @Override
    public boolean isSessionBlocklisted(String sessionId) {
        return false;
    }

    @Override
    public Set<String> getBlocklistedSessions(String userId) {
        return Set.of();
    }
}
