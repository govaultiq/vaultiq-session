package vaultiq.session.cache.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;

import java.util.List;

@Service
@ConditionalOnBean(VaultiqSessionCacheService.class)
public class VaultiqSessionManagerViaCache implements VaultiqSessionManager {

    private final VaultiqSessionCacheService vaultiqSessionCacheService;
    private final BlocklistSessionCacheService blocklistSessionCacheService;

    public VaultiqSessionManagerViaCache(
            VaultiqSessionCacheService vaultiqSessionCacheService,
            BlocklistSessionCacheService blocklistSessionCacheService) {
        this.vaultiqSessionCacheService = vaultiqSessionCacheService;
        this.blocklistSessionCacheService = blocklistSessionCacheService;
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

    @Override
    public void blocklistSession(String sessionId) {
        blocklistSessionCacheService.blocklistSession(sessionId);
    }
}
