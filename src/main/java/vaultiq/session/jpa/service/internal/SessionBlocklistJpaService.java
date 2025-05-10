
package vaultiq.session.jpa.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.jpa.model.JpaVaultiqSession;
import vaultiq.session.jpa.model.SessionBlocklistEntity;
import vaultiq.session.jpa.repository.SessionBlocklistRepository;
import vaultiq.session.jpa.repository.VaultiqSessionRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_JPA, type = ModelType.BLOCKLIST)
public class SessionBlocklistJpaService {
    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistJpaService.class);
    private final SessionBlocklistRepository sessionBlocklistRepository;
    private final VaultiqSessionRepository vaultiqSessionRepository;

    public SessionBlocklistJpaService(
            SessionBlocklistRepository sessionBlocklistRepository,
            VaultiqSessionRepository vaultiqSessionRepository
    ) {
        this.sessionBlocklistRepository = sessionBlocklistRepository;
        this.vaultiqSessionRepository = vaultiqSessionRepository;
    }

    /**
     * Blocklist (invalidate) all sessions for a given user.
     * Can be used to log out from all devices.
     *
     * @param userId the user identifier
     */
    @Transactional
    void blocklistAllSessions(String userId) {
        List<JpaVaultiqSession> sessions = vaultiqSessionRepository.findAllByUserId(userId);
        var sessionBlocklist = sessions.stream()
                .map(session -> SessionBlocklistEntity.create(session.getSessionId(), userId))
                .toList();
        sessionBlocklistRepository.saveAll(sessionBlocklist);
        log.info("Blocklisted all sessions for user '{}'.", userId);
    }

    /**
     * Blocklist (invalidate) all sessions except the specified session IDs.
     * Can be used to log out from all devices except, e.g., current device.
     *
     * @param userId             the user identifier
     * @param excludedSessionIds session IDs that should NOT be blocklisted
     */
    @Transactional
    void blocklistAllSessionsExcept(String userId, String... excludedSessionIds) {
        List<JpaVaultiqSession> sessions = vaultiqSessionRepository.findAllByUserId(userId);

        Set<String> excludedIds = excludedSessionIds == null
                ? Collections.emptySet()
                : new HashSet<>(Arrays.asList(excludedSessionIds));

        var sessionBlocklist = sessions.stream()
                .map(JpaVaultiqSession::getSessionId)
                .filter(sessionId -> !excludedIds.contains(sessionId))
                .map(sessionId -> SessionBlocklistEntity.create(sessionId, userId))
                .toList();

        sessionBlocklistRepository.saveAll(sessionBlocklist);
        log.info("Blocklisted all sessions for user '{}' except {}.", userId, excludedIds);
    }

    /**
     * Blocklist (invalidate) a specific session by session ID.
     * Can be used to log out from one device.
     *
     * @param sessionId the session identifier
     */
    @Transactional
    void blocklistSession(String sessionId) {
        Optional<JpaVaultiqSession> optionalSession =
                vaultiqSessionRepository.findById(sessionId);
        if (optionalSession.isPresent()) {
            JpaVaultiqSession session = optionalSession.get();
            SessionBlocklistEntity entity = SessionBlocklistEntity.create(sessionId, session.getUserId());
            sessionBlocklistRepository.save(entity);
            log.info("Blocklisted session '{}'.", sessionId);
        } else {
            log.warn("Attempted to blocklist non-existent session '{}'.", sessionId);
        }
    }

    /**
     * Check if a session is currently blocklisted.
     *
     * @param sessionId the session identifier
     * @return true if the session is blocklisted, false otherwise
     */
    boolean isSessionBlocklisted(String sessionId) {
        return sessionBlocklistRepository.existsById(sessionId);
    }

    /**
     * Get all blocklisted session IDs for a user.
     *
     * @param userId the user identifier
     * @return set of blocklisted session IDs for the user, or empty set if none
     */
    Set<String> getBlocklistedSessions(String userId) {
        return sessionBlocklistRepository.findAllByUserId(userId)
                .stream().map(SessionBlocklistEntity::getSessionId)
                .collect(Collectors.toSet());
    }
}
