package vaultiq.session.redis.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.CacheManager;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;

import java.util.List;

public class VaultiqSessionManagerViaRedis implements VaultiqSessionManager {

    private final CacheManager cacheManager;

    public VaultiqSessionManagerViaRedis(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
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
