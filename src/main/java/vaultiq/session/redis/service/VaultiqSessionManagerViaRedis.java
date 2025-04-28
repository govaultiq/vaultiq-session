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

import java.util.List;
import java.util.NoSuchElementException;
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
        cache.put(redisVaultiqSession.getSessionId(), redisVaultiqSession);

        return toVaultiqSession(redisVaultiqSession);
    }

    private Cache autoResolveCache() {
        var cacheName = Optional.ofNullable(RedisCacheContext.getCacheName())
                .orElseThrow(() -> new IllegalStateException("No cacheName set in RedisCacheContext. Please call RedisCacheContext.setCacheName() before using VaultiqSessionManagerViaRedis."));

        return Optional.ofNullable(cacheManager.getCache(cacheName))
                .orElseThrow(() -> new IllegalStateException("Cache not found by name: " + cacheName));
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

    private RedisVaultiqSession getRedisVaultiqSession(String sessionId) {
        var cache = this.autoResolveCache();
        return Optional.ofNullable(cache.get(sessionId, RedisVaultiqSession.class))
                .orElseThrow(() -> new NoSuchElementException("No session found with id: " + sessionId));
    }

    @Override
    public VaultiqSession getSession(String sessionId) {
        var session = this.getRedisVaultiqSession(sessionId);
        return this.toVaultiqSession(session);
    }

    @Override
    public void updateToCurrentlyActive(String sessionId) {
        var redisVaultiqSession = this.getRedisVaultiqSession(sessionId);
        redisVaultiqSession.currentlyActive();
        this.autoResolveCache().put(sessionId, redisVaultiqSession);
    }

    @Override
    public void deleteSession(String sessionId) {
        var cache = this.autoResolveCache();
        cache.evict(sessionId);
    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        return List.of();
    }
}
