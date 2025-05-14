package vaultiq.session.jpa.session.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.jpa.session.service.internal.VaultiqSessionService;

import java.util.List;

@Service
@ConditionalOnBean(VaultiqSessionService.class)
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_ONLY, type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
public class VaultiqSessionManagerViaJpa implements VaultiqSessionManager {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpa.class);

    private final VaultiqSessionService sessionService;

    public VaultiqSessionManagerViaJpa(VaultiqSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        log.debug("Creating session for user '{}' via JPA.", userId);
        return sessionService.create(userId, request);
    }

    @Override
    public VaultiqSession getSession(String sessionId) {
        log.debug("Retrieving session '{}' via JPA.", sessionId);
        return sessionService.get(sessionId);
    }

    @Override
    public void deleteSession(String sessionId) {
        log.debug("Deleting session '{}' via JPA.", sessionId);
        sessionService.delete(sessionId);
    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        log.debug("Listing sessions for user '{}' via JPA.", userId);
        return sessionService.list(userId);
    }

    @Override
    public int totalUserSessions(String userId) {
        log.debug("Counting sessions for user '{}' via JPA.", userId);
        return sessionService.count(userId);
    }
}