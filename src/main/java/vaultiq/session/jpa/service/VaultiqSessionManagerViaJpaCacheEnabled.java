package vaultiq.session.jpa.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.service.internal.BlocklistSessionCacheService;
import vaultiq.session.cache.service.internal.VaultiqSessionCacheService;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.jpa.service.internal.VaultiqSessionService;

import java.util.List;

@Service
@ConditionalOnBean({VaultiqSessionService.class, VaultiqSessionCacheService.class})
public class VaultiqSessionManagerViaJpaCacheEnabled implements VaultiqSessionManager {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpaCacheEnabled.class);

    private final VaultiqSessionService sessionService;
    private final VaultiqSessionCacheService cacheService;
    private final BlocklistSessionCacheService blocklistCacheService;

    public VaultiqSessionManagerViaJpaCacheEnabled(VaultiqSessionService sessionService,
                                                   VaultiqSessionCacheService cacheService,
                                                   BlocklistSessionCacheService blocklistCacheService) {
        this.sessionService = sessionService;
        this.cacheService = cacheService;
        this.blocklistCacheService = blocklistCacheService;
    }

    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        VaultiqSession session = sessionService.create(userId, request);
        log.debug("Storing newly created session '{}' in cache.", session.getSessionId());
        cacheService.cacheSession(session);
        return session;
    }

    @Override
    public VaultiqSession getSession(String sessionId) {
        VaultiqSession session = cacheService.getSession(sessionId);
        if (session == null) {
            log.debug("Session '{}' not found in cache. Fetching from DB.", sessionId);
            session = sessionService.get(sessionId);
            if (session != null) {
                cacheService.cacheSession(session);
            }
        }
        return session;
    }

    @Override
    public void deleteSession(String sessionId) {
        log.debug("Deleting session '{}' from DB and cache.", sessionId);
        VaultiqSession session = getSession(sessionId);
        if (session != null) {
            sessionService.delete(sessionId);
            cacheService.deleteSession(sessionId);
        } else {
            log.debug("Session '{}' not found while trying to delete.", sessionId);
        }
    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        var sessions = cacheService.getSessionsByUser(userId);

        if (sessions.isEmpty()) {
            sessions = sessionService.list(userId);
            cacheService.cacheUserSessions(userId, sessions);
        }
        return sessions;
    }

    @Override
    public int totalUserSessions(String userId) {
        List<String> sessionIds = cacheService.getUserSessionIds(userId);
        if (!sessionIds.isEmpty()) {
            return sessionIds.size();
        } else {
            return sessionService.count(userId);
        }
    }
}
