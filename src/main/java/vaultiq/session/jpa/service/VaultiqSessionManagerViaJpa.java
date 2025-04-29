package vaultiq.session.jpa.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.jpa.repository.VaultiqSessionRepository;
import vaultiq.session.jpa.model.JpaVaultiqSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnBean(VaultiqSessionRepository.class)
@ConditionalOnProperty(
        prefix = "vaultiq.session.persistence.via-jpa",
        name = "allow-inflight-entity-creation",
        havingValue = "true")
public class VaultiqSessionManagerViaJpa implements VaultiqSessionManager {

    private final VaultiqSessionRepository sessionRepository;
    private final DeviceFingerprintGenerator fingerprintGenerator;
    private final Boolean isCachingEnabled;
    private final String cacheManagerName;
    private final CacheManager vaultiqCacheManager;

    public VaultiqSessionManagerViaJpa(
            VaultiqSessionRepository sessionRepository,
            DeviceFingerprintGenerator fingerprintGenerator,
            VaultiqSessionProperties props,
            @Qualifier("vaultiqCacheManager") @Nullable CacheManager vaultiqCacheManager) {
        this.sessionRepository = sessionRepository;
        this.fingerprintGenerator = fingerprintGenerator;
        this.isCachingEnabled = props.getViaJpa().isEnableCaching();
        this.cacheManagerName = props.getViaJpa().getCacheManagerName();
        this.vaultiqCacheManager = vaultiqCacheManager;
    }

    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        String deviceFingerPrint = fingerprintGenerator.generateFingerprint(request);
        Instant now = Instant.now();

        JpaVaultiqSession entity = new JpaVaultiqSession();
        entity.setUserId(userId);
        entity.setDeviceFingerPrint(deviceFingerPrint);
        entity.setCreatedAt(now);
        entity.setLastActiveAt(now);

        entity = sessionRepository.save(entity);
        VaultiqSession session = mapToVaultiqSession(entity);

        // Save in cache as well
        if (isCachingEnabled) {
            cacheSession(session);
            evictUserSessions(entity.getUserId());
        }

        return session;
    }

    @Override
    public VaultiqSession getSession(String sessionId) {
        VaultiqSession session = null;
        if (isCachingEnabled) {
            session = getSessionViaCache(sessionId);
        }
        return Optional.ofNullable(session)
                .orElseGet(() -> sessionRepository.findById(sessionId)
                        .map(entity -> {
                            VaultiqSession vs = mapToVaultiqSession(entity);
                            if (isCachingEnabled) cacheSession(vs);
                            return vs;
                        }).orElse(null));
    }

    @Override
    public void updateToCurrentlyActive(String sessionId) {
        sessionRepository.findById(sessionId)
                .ifPresent(entity -> {
                    entity.setLastActiveAt(Instant.now());
                    JpaVaultiqSession updated = sessionRepository.save(entity);
                    VaultiqSession session = mapToVaultiqSession(updated);

                    if (isCachingEnabled) {
                        cacheSession(session);
                        evictUserSessions(entity.getUserId());
                    }
                });
    }

    public void deleteSession(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(entity -> {
            sessionRepository.delete(entity);
            evictUserSessions(entity.getUserId());
        });
    }

    public void evictUserSessions(String userId) {
        if (isCachingEnabled) {
            getCache().ifPresent(cache -> cache.evict(userSessionCacheKey(userId)));
        }
    }

    private static String userSessionCacheKey(String userId) {
        return "userSessions:" + userId;
    }

    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        List<VaultiqSession> sessions = null;

        if(isCachingEnabled)
            sessions = getCachedUserSessions(userId);

        sessions = Optional.ofNullable(sessions)
                .orElseGet(() -> sessionRepository.findAllByUserId(userId).stream()
                        .map(this::mapToVaultiqSession)
                        .toList());

        if (isCachingEnabled && !sessions.isEmpty()) {
            List<VaultiqSession> finalSessions = sessions;
            getCache().ifPresent(cache -> cache.put(userSessionCacheKey(userId), finalSessions));
        }

        return sessions;
    }

    @SuppressWarnings("unchecked")
    private List<VaultiqSession> getCachedUserSessions(String userId) {
        return getCache()
                .map(cache -> cache.get(userSessionCacheKey(userId), List.class))
                .orElse(null);
    }

    /**
     * Maps JPA session entity to domain VaultiqSession.
     */
    private VaultiqSession mapToVaultiqSession(JpaVaultiqSession entity) {
        return VaultiqSession.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .deviceFingerPrint(entity.getDeviceFingerPrint())
                .createdAt(entity.getCreatedAt())
                .lastActiveAt(entity.getLastActiveAt())
                .build();
    }

    /**
     * Helper method to put session into cache after manual changes.
     */
    private void cacheSession(VaultiqSession session) {
        getCache().ifPresent(cache -> cache.put(session.getSessionId(), session));
    }

    private VaultiqSession getSessionViaCache(String sessionId) {
        return getCache()
                .map(cache -> cache.get(sessionId, VaultiqSession.class))
                .orElse(null);
    }

    private Optional<Cache> getCache() {
        return Optional.ofNullable(vaultiqCacheManager)
                .flatMap(cacheManager -> Optional.ofNullable(cacheManager.getCache("vaultiq-session-pool")));
    }
}
