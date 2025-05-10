package vaultiq.session.cache.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.cache.service.internal.VaultiqSessionCacheService;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;

import java.util.List;

@Service
@ConditionalOnBean(VaultiqSessionCacheService.class)
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.CACHE_ONLY, type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
public class VaultiqSessionManagerViaCache implements VaultiqSessionManager {

    private final VaultiqSessionCacheService vaultiqSessionCacheService;

    public VaultiqSessionManagerViaCache(VaultiqSessionCacheService vaultiqSessionCacheService) {
        this.vaultiqSessionCacheService = vaultiqSessionCacheService;
    }

    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        return vaultiqSessionCacheService.createSession(userId, request);
    }

    @Override
    public VaultiqSession getSession(String sessionId) {
        return vaultiqSessionCacheService.getSession(sessionId);
    }

    @Override
    public void deleteSession(String sessionId) {
        vaultiqSessionCacheService.deleteSession(sessionId);
    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        return vaultiqSessionCacheService.getSessionsByUser(userId);
    }

    @Override
    public int totalUserSessions(String userId) {
        return vaultiqSessionCacheService.totalUserSessions(userId);
    }
}
