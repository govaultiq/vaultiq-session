package vaultiq.session.jpa.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import vaultiq.session.config.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.VaultiqPersistenceMode;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;

import java.util.List;

@Service
@CacheConfig(
        cacheNames = "${vaultiq.session.persistence.cache.cache-names.sessions}",
        cacheManager = "${vaultiq.session.persistence.cache.manager}"
)
@ConditionalOnBean(VaultiqSessionService.class)
@ConditionalOnVaultiqPersistence(VaultiqPersistenceMode.JPA_AND_CACHE)
public class VaultiqSessionManagerViaJpaCacheEnabled implements VaultiqSessionManager {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpaCacheEnabled.class);

    private final VaultiqSessionService sessionService;

    public VaultiqSessionManagerViaJpaCacheEnabled(
            VaultiqSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    @CachePut(key = "#result.sessionId")
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        VaultiqSession session = sessionService.create(userId, request);
        log.debug("Storing newly created session '{}' in cache.", session.getSessionId());
        return session;
    }

    @Override
    @Cacheable(key = "#sessionId")
    public VaultiqSession getSession(String sessionId) {
        log.debug("Fetching session '{}' from cache or DB.", sessionId);
        return sessionService.get(sessionId);
    }

    @CachePut(key = "#result.sessionId")
    public void updateToCurrentlyActive(String sessionId) {
        log.debug("Updating lastActiveAt status for session '{}' in cache.", sessionId);
        sessionService.touch(sessionId);
    }

    @Override
    @CacheEvict(key = "#sessionId")
    public void deleteSession(String sessionId) {
        log.debug("Deleting session '{}' and evicting from cache.", sessionId);

        var session = this.getSession(sessionId);

        if (session != null) {
            sessionService.delete(sessionId);
            this.evictUserSessions(session.getUserId());
        } else {
            log.debug("Session '{}' not found while trying to delete.", sessionId);
        }

    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        log.debug("Fetching sessions for user '{}'.", userId);
        var sessionIds = this.getSessionIdsByUser(userId);

        if (!sessionIds.isEmpty()) {
            return sessionIds.stream()
                    .map(this::getSession)
                    .toList();
        } else {
            return sessionService.list(userId);
        }
    }

    @Override
    public int totalUserSessions(String userId) {
        log.debug("Counting sessions for user '{}'.", userId);
        var sessionIds = this.getSessionIdsByUser(userId);

        if (!sessionIds.isEmpty()) {
            return sessionIds.size();
        } else {
            return sessionService.count(userId);
        }
    }

    @Cacheable(key = "'user_sessions_' + #userId")
    public List<String> getSessionIdsByUser(String userId) {
        return sessionService.list(userId)
                .stream()
                .map(VaultiqSession::getSessionId)
                .toList();
    }

    @CacheEvict(key = "'user_sessions_' + #userId")
    public void evictUserSessions(String userId) {
        log.debug("Evicting all sessions for user '{}'.", userId);
    }
}