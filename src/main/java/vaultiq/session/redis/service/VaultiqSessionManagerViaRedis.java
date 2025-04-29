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
                .orElseThrow(() -> {
                    log.error("CacheManager '{}' not found in provided cacheManagers map.", cacheManagerName);
                    return new IllegalStateException("Required CacheManager '" + cacheManagerName + "' not found.");
                });

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
        var session = Optional.ofNullable(this.getUserSessionPool())
                .map(sessionPool -> sessionPool.getSession(sessionId))
                .map(this::toVaultiqSession)
                .orElse(null);
        log.debug("Retrieved sessionId={} from session pool, found={}", sessionId, session != null);
        return session;
    }

    @Override
    public void updateToCurrentlyActive(String sessionId) {
        Optional.ofNullable(this.getUserSessionPool())
                .ifPresent(sessionPool -> {
                    var session = sessionPool.getSession(sessionId);

                    Optional.ofNullable(session)
                            .ifPresent(s -> {
                                s.currentlyActive();
                                log.info("Marked sessionId={} as currently active", sessionId);
                            });

                    putSessionPool(sessionPool);
                });
    }

    @Override
    public void deleteSession(String sessionId) {
        Optional.ofNullable(this.getUserSessionPool())
                .ifPresent(sessionPool -> {
                    sessionPool.deleteSession(sessionId);
                    log.info("Deleted sessionId={} from session pool", sessionId);
                    putSessionPool(sessionPool);
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
        var cacheName = Optional.ofNullable(RedisCacheContext.getCacheName())
                .orElseThrow(() -> {
                    log.error("Cache name not set in RedisCacheContext.");
                    return new IllegalStateException("No cacheName set in RedisCacheContext. Please call RedisCacheContext.setCacheName() before using VaultiqSessionManagerViaRedis.");
                });

        return Optional.ofNullable(cacheManager.getCache(cacheName))
                .orElseThrow(() -> {
                    log.error("Cache '{}' not found in CacheManager.", cacheName);
                    return new IllegalStateException("Cache not found by name: " + cacheName);
                });
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
