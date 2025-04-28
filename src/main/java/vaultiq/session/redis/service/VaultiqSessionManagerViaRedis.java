package vaultiq.session.redis.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.redis.model.RedisVaultiqSession;
import vaultiq.session.redis.model.UserSessionPool;

import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnBean(name = "vaultiqCacheManager")
@ConditionalOnProperty(prefix = "vaultiq.session.persistence.via-redis",
        name = "allow-inflight-cache-management",
        havingValue = "true")
public class VaultiqSessionManagerViaRedis implements VaultiqSessionManager {

    private final CacheManager cacheManager;
    private final DeviceFingerprintGenerator fingerprintGenerator;

    public VaultiqSessionManagerViaRedis(
            @Qualifier("vaultiqCacheManager") CacheManager cacheManager,
            DeviceFingerprintGenerator fingerprintGenerator) {
        this.cacheManager = cacheManager;
        this.fingerprintGenerator = fingerprintGenerator;
    }

    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) throws IllegalStateException {
        var fingerprint = fingerprintGenerator.generateFingerprint(request);
        var redisVaultiqSession = RedisVaultiqSession.create(userId, fingerprint);

        var cache = autoResolveCache();
        var sessionPool = Optional.ofNullable(this.getUserSessionPool())
                .orElseGet(UserSessionPool::createEmpty);

        sessionPool.addSession(redisVaultiqSession);
        cache.put(userId, sessionPool);

        return toVaultiqSession(redisVaultiqSession);
    }

    private Cache autoResolveCache() {
        var cacheName = Optional.ofNullable(RedisCacheContext.getCacheName())
                .orElseThrow(() -> new IllegalStateException("No cacheName set in RedisCacheContext. Please call RedisCacheContext.setCacheName() before using VaultiqSessionManagerViaRedis."));

        return Optional.ofNullable(cacheManager.getCache(cacheName))
                .orElseThrow(() -> new IllegalStateException("Cache not found by name: " + cacheName));
    }

    private UserSessionPool getUserSessionPool() {
        var userId = Optional.ofNullable(RedisCacheContext.getUserId())
                .orElseThrow(() -> new IllegalStateException("No userId set in RedisCacheContext. Please call RedisCacheContext.setUserId() before using VaultiqSessionManagerViaRedis."));
        var cache = this.autoResolveCache();
        return cache.get(userId, UserSessionPool.class);
    }

    private VaultiqSession toVaultiqSession(RedisVaultiqSession source) {
        return VaultiqSession.builder()
                .sessionId(source.getSessionId())
                .userId(source.getUserId())
                .deviceFingerPrint(source.getDeviceFingerPrint())
                .createdAt(source.getCreatedAt())
                .lastActiveAt(source.getLastActiveAt())
                .build();
    }

    @Override
    public VaultiqSession getSession(String sessionId) {
        return Optional.ofNullable(this.getUserSessionPool())
                .map(sessionPool -> sessionPool.getSession(sessionId))
                .map(this::toVaultiqSession)
                .orElse(null);
    }

    @Override
    public void updateToCurrentlyActive(String sessionId) {
        Optional.ofNullable(this.getUserSessionPool())
                .ifPresent(sessionPool -> {
                    var session = sessionPool.getSession(sessionId);

                    Optional.ofNullable(session)
                            .ifPresent(RedisVaultiqSession::currentlyActive);

                    putSessionPool(sessionPool);
                });
    }

    @Override
    public void deleteSession(String sessionId) {
        Optional.ofNullable(this.getUserSessionPool())
                .ifPresent(sessionPool -> {
                    sessionPool.deleteSession(sessionId);
                    putSessionPool(sessionPool);
                });
    }

    private void putSessionPool(UserSessionPool sessionPool) {
        Optional.ofNullable(RedisCacheContext.getUserId())
                .ifPresent(userId -> this.autoResolveCache().put(userId, sessionPool));
    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        return this.getUserSessionPool().getSessions()
                .stream()
                .map(this::toVaultiqSession)
                .toList();
    }
}
