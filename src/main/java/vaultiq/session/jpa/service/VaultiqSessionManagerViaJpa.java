package vaultiq.session.jpa.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.jpa.repository.VaultiqSessionRepository;

import java.util.List;

@Service
@ConditionalOnBean(VaultiqSessionRepository.class)
@ConditionalOnProperty(
        prefix = "vaultiq.session.persistence.via-jpa",
        name = "allow-inflight-entity-creation",
        havingValue = "true")
public class VaultiqSessionManagerViaJpa implements VaultiqSessionManager {

    private final VaultiqSessionRepository sessionRepository;

    public VaultiqSessionManagerViaJpa(VaultiqSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        return null;
    }

    @Override
    public VaultiqSession getSession(String sessionId) {
        return null;
    }

    @Override
    public void updateToCurrentlyActive(String sessionId) {

    }

    @Override
    public void deleteSession(String sessionId) {

    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        return List.of();
    }
}
