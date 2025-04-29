package vaultiq.session.jpa.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.jpa.repository.VaultiqSessionRepository;
import vaultiq.session.jpa.model.JpaVaultiqSession;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnBean(VaultiqSessionRepository.class)
@ConditionalOnProperty(
        prefix = "vaultiq.session.persistence.via-jpa",
        name = "allow-inflight-entity-creation",
        havingValue = "true")
public class VaultiqSessionManagerViaJpa implements VaultiqSessionManager {

    private final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpa.class);

    private final VaultiqSessionRepository sessionRepository;
    private final DeviceFingerprintGenerator fingerprintGenerator;
    private final Boolean isCachingEnabled;
    private final String cacheName;
    private final CacheManager vaultiqCacheManager;

    public VaultiqSessionManagerViaJpa(
            VaultiqSessionRepository sessionRepository,
            DeviceFingerprintGenerator fingerprintGenerator,
            VaultiqSessionProperties props,
            Map<String, CacheManager> cacheManagers) {

        this.sessionRepository = sessionRepository;
        this.fingerprintGenerator = fingerprintGenerator;
        this.isCachingEnabled = props.getViaJpa().isEnableCaching();
        this.cacheName = props.getViaJpa().getCacheName();

        String cacheManagerName = props.getViaJpa().getCacheManagerName();
        this.vaultiqCacheManager = Optional.ofNullable(cacheManagerName)
                .map(cacheManagers::get)
                .orElse(null);

        if (isCachingEnabled) {
            if (vaultiqCacheManager == null) {
                log.warn("Caching is enabled but no CacheManager named '{}' was found.", cacheManagerName);
            } else {
                log.info("Caching is enabled using CacheManager '{}', targeting cache '{}'.", cacheManagerName, cacheName);
            }
        } else {
            log.info("Session caching is disabled via configuration.");
        }
    }

    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        log.debug("Creating session for user '{}'.", userId);

        String deviceFingerPrint = fingerprintGenerator.generateFingerprint(request);
        Instant now = Instant.now();

        JpaVaultiqSession entity = new JpaVaultiqSession();
        entity.setUserId(userId);
        entity.setDeviceFingerPrint(deviceFingerPrint);
        entity.setCreatedAt(now);
        entity.setLastActiveAt(now);

        entity = sessionRepository.save(entity);
        log.info("Persisted new session '{}' for user '{}'.", entity.getSessionId(), userId);

        VaultiqSession session = mapToVaultiqSession(entity);

        if (isCachingEnabled) {
            cacheSession(session);
            evictUserSessions(userId);
        }

        return session;
    }

    @Override
    public VaultiqSession getSession(String sessionId) {
        log.debug("Retrieving session '{}'.", sessionId);
        VaultiqSession session = null;
        if (isCachingEnabled) {
            session = getSessionViaCache(sessionId);
            if (session != null) log.debug("Session '{}' loaded from cache.", sessionId);
        }

        if (session == null) {
            session = sessionRepository.findById(sessionId)
                    .map(entity -> {
                        VaultiqSession vs = mapToVaultiqSession(entity);
                        log.debug("Session '{}' loaded from database.", sessionId);
                        if (isCachingEnabled) cacheSession(vs);
                        return vs;
                    }).orElse(null);
            if (session == null) log.info("Session '{}' not found in database.", sessionId);
        }

        return session;
    }

    @Override
    public void updateToCurrentlyActive(String sessionId) {
        sessionRepository.findById(sessionId)
                .ifPresent(entity -> {
                    entity.setLastActiveAt(Instant.now());
                    JpaVaultiqSession updated = sessionRepository.save(entity);
                    VaultiqSession session = mapToVaultiqSession(updated);
                    log.info("Updated 'lastActiveAt' for session '{}'.", sessionId);

                    if (isCachingEnabled) {
                        cacheSession(session);
                        evictUserSessions(entity.getUserId());
                    }
                });
    }

    public void deleteSession(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(entity -> {
            sessionRepository.delete(entity);
            log.info("Deleted session '{}' for user '{}'.", sessionId, entity.getUserId());
            evictUserSessions(entity.getUserId());
        });
    }

    private void evictUserSessions(String userId) {
        if (isCachingEnabled) {
            getCache().ifPresent(cache -> {
                cache.evict(userSessionCacheKey(userId));
                log.debug("Evicted cached user sessions for user '{}'.", userId);
            });
        }
    }

    private static String userSessionCacheKey(String userId) {
        return "userSessions:" + userId;
    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        log.debug("Fetching sessions for user '{}'.", userId);

        List<VaultiqSession> sessions = null;
        if (isCachingEnabled)
            sessions = getCachedUserSessions(userId);

        sessions = Optional.ofNullable(sessions)
                .orElseGet(() -> sessionRepository.findAllByUserId(userId).stream()
                        .map(this::mapToVaultiqSession)
                        .toList());

        if (isCachingEnabled && !sessions.isEmpty()) {
            List<VaultiqSession> finalSessions = sessions;
            getCache().ifPresent(cache -> {
                cache.put(userSessionCacheKey(userId), finalSessions);
                log.debug("Cached sessions for user '{}'.", userId);
            });
        }

        return sessions;
    }

    @SuppressWarnings("unchecked")
    private List<VaultiqSession> getCachedUserSessions(String userId) {
        return getCache()
                .map(cache -> cache.get(userSessionCacheKey(userId), List.class))
                .orElse(null);
    }

    private VaultiqSession mapToVaultiqSession(JpaVaultiqSession entity) {
        return VaultiqSession.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .deviceFingerPrint(entity.getDeviceFingerPrint())
                .createdAt(entity.getCreatedAt())
                .lastActiveAt(entity.getLastActiveAt())
                .build();
    }

    private void cacheSession(VaultiqSession session) {
        getCache().ifPresent(cache -> {
            cache.put(session.getSessionId(), session);
            log.debug("Cached session '{}'.", session.getSessionId());
        });
    }

    private VaultiqSession getSessionViaCache(String sessionId) {
        return getCache()
                .map(cache -> cache.get(sessionId, VaultiqSession.class))
                .orElse(null);
    }

    private Optional<Cache> getCache() {
        return Optional.ofNullable(vaultiqCacheManager)
                .map(cm -> {
                    Cache cache = cm.getCache(cacheName);
                    if (cache == null) {
                        log.warn("Cache '{}' not found in CacheManager '{}'.", cacheName, cm.getClass().getSimpleName());
                    }
                    return cache;
                });
    }
}