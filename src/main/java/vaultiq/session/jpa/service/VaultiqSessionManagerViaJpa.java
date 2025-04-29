package vaultiq.session.jpa.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import vaultiq.session.core.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.jpa.repository.VaultiqSessionRepository;
import vaultiq.session.jpa.model.JpaVaultiqSession;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnBean(VaultiqSessionRepository.class)
@ConditionalOnProperty(
        prefix = "vaultiq.session.persistence.via-jpa",
        name = "allow-inflight-entity-creation",
        havingValue = "true")
public class VaultiqSessionManagerViaJpa implements VaultiqSessionManager {

    private final VaultiqSessionRepository sessionRepository;
    private final DeviceFingerprintGenerator fingerprintGenerator;

    public VaultiqSessionManagerViaJpa(
            VaultiqSessionRepository sessionRepository,
            DeviceFingerprintGenerator fingerprintGenerator) {
        this.sessionRepository = sessionRepository;
        this.fingerprintGenerator = fingerprintGenerator;
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
        cacheSession(session);
        evictUserSessions(entity.getUserId());
        return session;
    }

    @Override
    @Cacheable(
            cacheNames = "vaultiq-session-pool",
            key = "#sessionId",
            cacheManager = "vaultiqCacheManager"
    )
    public VaultiqSession getSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .map(this::mapToVaultiqSession)
                .orElse(null);
    }

    @Override
    public void updateToCurrentlyActive(String sessionId) {
        sessionRepository.findById(sessionId)
                .ifPresent(entity -> {
                    entity.setLastActiveAt(Instant.now());
                    JpaVaultiqSession updated = sessionRepository.save(entity);
                    VaultiqSession session = mapToVaultiqSession(updated);
                    // Update the cache too
                    cacheSession(session);
                    evictUserSessions(entity.getUserId());
                });
    }

    @Override
    @CacheEvict(
            cacheNames = "vaultiq-session-pool",
            key = "#sessionId",
            cacheManager = "vaultiqCacheManager"
    )
    public void deleteSession(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(entity -> {
            sessionRepository.delete(entity);
            evictUserSessions(entity.getUserId());
        });
    }

    @CacheEvict(
            cacheNames = "vaultiq-session-pool",
            key = "'userSessions:' + #userId",
            cacheManager = "vaultiqCacheManager"
    )
    public void evictUserSessions(String userId) {
        // no implementation, the method evicts cache based on userId
    }

    @Override
    @Cacheable(
            cacheNames = "vaultiq-session-pool",
            key = "'userSessions:' + #userId",
            cacheManager = "vaultiqCacheManager"
    )
    public List<VaultiqSession> getSessionsByUser(String userId) {
        List<JpaVaultiqSession> entities = sessionRepository.findAllByUserId(userId);
        return entities.stream()
                .map(this::mapToVaultiqSession)
                .collect(Collectors.toList());
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
    @CachePut(
            cacheNames = "vaultiq-session-pool",
            key = "#session.sessionId",
            cacheManager = "vaultiqCacheManager"
    )
    public VaultiqSession cacheSession(VaultiqSession session) {
        return session;
    }
}
