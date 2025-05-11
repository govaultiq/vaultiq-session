package vaultiq.session.jpa.service.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;
import vaultiq.session.jpa.model.JpaVaultiqSession;
import vaultiq.session.jpa.repository.VaultiqSessionRepository;

import java.time.Instant;
import java.util.List;

@Service
@ConditionalOnBean(VaultiqSessionRepository.class)
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_JPA, type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
public class VaultiqSessionService {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionService.class);
    private final VaultiqSessionRepository sessionRepository;
    private final DeviceFingerprintGenerator fingerprintGenerator;

    public VaultiqSessionService(
            VaultiqSessionRepository sessionRepository,
            DeviceFingerprintGenerator fingerprintGenerator) {
        this.sessionRepository = sessionRepository;
        this.fingerprintGenerator = fingerprintGenerator;
    }

    public VaultiqSession create(String userId, HttpServletRequest request) {
        log.debug("Creating session for user '{}'.", userId);

        String deviceFingerPrint = fingerprintGenerator.generateFingerprint(request);
        Instant now = Instant.now();

        JpaVaultiqSession entity = new JpaVaultiqSession();
        entity.setUserId(userId);
        entity.setDeviceFingerPrint(deviceFingerPrint);
        entity.setCreatedAt(now);

        entity = sessionRepository.save(entity);
        log.info("Persisted new session '{}' for user '{}'.", entity.getSessionId(), userId);

        return mapToVaultiqSession(entity);
    }

    public VaultiqSession get(String sessionId) {
        log.debug("Retrieving session '{}'.", sessionId);

        VaultiqSession session = sessionRepository.findById(sessionId)
                .map(this::mapToVaultiqSession).orElse(null);

        if (session == null) log.info("Session '{}' not found in database.", sessionId);
        else log.debug("Session '{}' loaded from database.", sessionId);

        return session;
    }

    public void delete(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(entity -> {
            sessionRepository.delete(entity);
            log.info("Deleted session '{}' for user '{}'.", sessionId, entity.getUserId());
        });
    }

    public List<VaultiqSession> list(String userId) {
        log.debug("Fetching sessions for user '{}'.", userId);

        return sessionRepository.findAllByUserId(userId).stream()
                        .map(this::mapToVaultiqSession)
                        .toList();
    }

    public int count(String userId) {
        return sessionRepository.countByUserId(userId);
    }

    private VaultiqSession mapToVaultiqSession(JpaVaultiqSession entity) {
        return VaultiqSession.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .deviceFingerPrint(entity.getDeviceFingerPrint())
                .createdAt(entity.getCreatedAt())
                .isBlocked(entity.isBlocked())
                .blockedAt(entity.getBlockedAt())
                .build();
    }
}
