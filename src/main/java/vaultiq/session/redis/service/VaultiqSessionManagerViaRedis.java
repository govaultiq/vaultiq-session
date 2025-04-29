package vaultiq.session.redis.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.redis.model.RedisVaultiqSession;
import vaultiq.session.redis.model.UserSessionPool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "vaultiq.session.persistence.via-redis",
        name = "allow-inflight-cache-management",
        havingValue = "true")
public class VaultiqSessionManagerViaRedis implements VaultiqSessionManager {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaRedis.class);

    private final CacheManager cacheManager;
    private final DeviceFingerprintGenerator fingerprintGenerator;

    public VaultiqSessionManagerViaRedis(
            VaultiqSessionProperties props,
            Map<String, CacheManager> cacheManagers,
            DeviceFingerprintGenerator fingerprintGenerator) {

        String cacheManagerName = props.getViaRedis().getCacheManagerName();
        this.cacheManager = Optional.ofNullable(cacheManagerName)
                .map(cacheManagers::get)
                .orElse(null);

        if (cacheManager == null) {
            log.warn("CacheManager '{}' not found, Redis session caching may fail.", cacheManagerName);
        }

        this.fingerprintGenerator = fingerprintGenerator;
    }

    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) throws IllegalStateException {
        var fingerprint = fingerprintGenerator.generateFingerprint(request);
        var redisVaultiqSession = RedisVaultiqSession.create(userId, fingerprint);
        log.info("Creating session for userId={}, deviceFingerprint={}", userId, fingerprint);

        var cache = autoResolveCache();
        var sessionPool = Optional.ofNullable(this.getUserSessionPool())
                .orElseGet(() -> {
                    log.debug("No session pool found for userId={}, creating new pool.", userId);
                    return UserSessionPool.createEmpty();
                });

        sessionPool.addSession(redisVaultiqSession);
        cache.put(userId, sessionPool);
        log.debug("Session added to pool and cached for userId={}, sessionId={}", userId, redisVaultiqSession.getSessionId());

        return toVaultiqSession(redisVaultiqSession);
    }

    @Override
    public VaultiqSession getSession(String sessionId) {
        var userSessions = this.getUserSessionPool();
        var session = Optional.ofNullable(userSessions.getSession(sessionId))
                .map(this::toVaultiqSession)
                .orElse(null);

        log.debug("Retrieved sessionId={} from session pool, found={}", sessionId, session != null);
        return session;
    }

    @Override
    public void updateToCurrentlyActive(String sessionId) {
        var userSessions = this.getUserSessionPool();
        Optional.ofNullable(userSessions.getSession(sessionId))
                .ifPresent(s -> {
                    s.currentlyActive();
                    putSessionPool(userSessions);
                    log.info("Marked sessionId={} as currently active", sessionId);
                });
    }

    @Override
    public void deleteSession(String sessionId) {
        var userSessions = this.getUserSessionPool();
        Optional.ofNullable(userSessions.getSession(sessionId))
                .ifPresent(s -> {
                    userSessions.deleteSession(sessionId);
                    putSessionPool(userSessions);
                    log.info("Deleted sessionId={} from session pool", sessionId);
                });
    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        var sessions = this.getUserSessionPool().getSessions()
                .stream()
                .map(this::toVaultiqSession)
                .toList();
        log.debug("Fetched {} sessions for userId={}", sessions.size(), userId);
        return sessions;
    }

    private Cache autoResolveCache() {
        var cacheName = RedisCacheContext.getCacheName();
        if (cacheName == null) {
            log.error("Cache name not set in RedisCacheContext.");
            throw new IllegalStateException("No cacheName set in RedisCacheContext.");
        }

        var cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.error("Cache '{}' not found in CacheManager.", cacheName);
            throw new IllegalStateException("Cache not found by name: " + cacheName);
        }
        return cache;
    }

    private UserSessionPool getUserSessionPool() {
        var userId = Optional.ofNullable(RedisCacheContext.getUserId())
                .orElseThrow(() -> {
                    log.error("User ID not set in RedisCacheContext.");
                    return new IllegalStateException("No userId set in RedisCacheContext. Please call RedisCacheContext.setUserId() before using VaultiqSessionManagerViaRedis.");
                });
        var cache = this.autoResolveCache();
        var pool = cache.get(userId, UserSessionPool.class);
        log.debug("Fetched session pool for userId={}, found={}", userId, pool != null);
        return pool;
    }

    private void putSessionPool(UserSessionPool sessionPool) {
        Optional.ofNullable(RedisCacheContext.getUserId())
                .ifPresent(userId -> {
                    this.autoResolveCache().put(userId, sessionPool);
                    log.debug("Updated session pool in cache for userId={}", userId);
                });
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
}
