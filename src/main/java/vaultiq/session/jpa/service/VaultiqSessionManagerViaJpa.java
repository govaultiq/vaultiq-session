package vaultiq.session.jpa.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.config.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.VaultiqPersistenceMode;
import vaultiq.session.core.VaultiqSession;

import java.util.List;

@Service
@ConditionalOnBean(VaultiqSessionService.class)
@ConditionalOnVaultiqPersistence(VaultiqPersistenceMode.JPA_ONLY)
public class VaultiqSessionManagerViaJpa {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpa.class);

    private final VaultiqSessionService sessionService;

    public VaultiqSessionManagerViaJpa(VaultiqSessionService sessionService) {
        this.sessionService = sessionService;
    }

    public VaultiqSession create(String userId, HttpServletRequest request) {
        log.debug("Creating session for user '{}' via JPA.", userId);
        return sessionService.create(userId, request);
    }

    public VaultiqSession get(String sessionId) {
        log.debug("Retrieving session '{}' via JPA.", sessionId);
        return sessionService.get(sessionId);
    }

    public VaultiqSession touch(String sessionId) {
        log.debug("Updating lastActiveAt status for session '{}' via JPA.", sessionId);
        return sessionService.touch(sessionId);
    }

    public void delete(String sessionId) {
        log.debug("Deleting session '{}' via JPA.", sessionId);
        sessionService.delete(sessionId);
    }

    public List<VaultiqSession> list(String userId) {
        log.debug("Listing sessions for user '{}' via JPA.", userId);
        return sessionService.list(userId);
    }

    public int count(String userId) {
        log.debug("Counting sessions for user '{}' via JPA.", userId);
        return sessionService.count(userId);
    }
}