package vaultiq.session.jpa.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import vaultiq.session.config.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.VaultiqPersistenceMode;
import vaultiq.session.core.VaultiqSession;

import java.util.List;

@Service
@CacheConfig(
        cacheNames    = "${vaultiq.session.persistence.jpa.session-pool}",
        cacheManager  = "${vaultiq.session.persistence.jpa.manager}"
)
@ConditionalOnBean(VaultiqSessionService.class)
@ConditionalOnVaultiqPersistence(VaultiqPersistenceMode.JPA_AND_CACHE)
public class VaultiqSessionManagerViaJpaCacheEnabled {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpaCacheEnabled.class);

    private final VaultiqSessionService sessionService;

    public VaultiqSessionManagerViaJpaCacheEnabled(
            VaultiqSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @CachePut(key = "#result.sessionId")
    public VaultiqSession create(String userId, HttpServletRequest request) {
        VaultiqSession session = sessionService.create(userId, request);
        log.debug("Storing newly created session '{}' in cache.", session.getSessionId());
        return session;
    }

    @Cacheable(key = "#sessionId")
    public VaultiqSession get(String sessionId) {
        log.debug("Fetching session '{}' from cache or DB.", sessionId);
        return sessionService.get(sessionId);
    }

    @CachePut(key = "#session.sessionId")
    public VaultiqSession touch(String sessionId) {
        log.debug("Updating lastActiveAt status for session '{}' in cache.", sessionId);
        return sessionService.touch(sessionId);
    }

    @CacheEvict(key = "#sessionId")
    public void delete(String sessionId) {
        log.debug("Deleting session '{}' and evicting from cache.", sessionId);
        sessionService.delete(sessionId);
    }

    public List<VaultiqSession> list(String userId) {
        return sessionService.list(userId);
    }

    public int count(String userId) {
        return sessionService.count(userId);
    }
}