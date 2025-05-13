
package vaultiq.session.jpa.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.core.contracts.UserIdentityAware;
import vaultiq.session.core.model.RevocationType;
import vaultiq.session.jpa.model.JpaVaultiqSession;
import vaultiq.session.jpa.model.SessionBlocklistEntity;
import vaultiq.session.jpa.repository.SessionBlocklistRepository;
import vaultiq.session.jpa.repository.VaultiqSessionRepository;
import vaultiq.session.jpa.util.BlocklistContext;

import java.time.Instant;
import java.util.*;

@Service
@ConditionalOnBean(UserIdentityAware.class)
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_JPA, type = ModelType.BLOCKLIST)
public class SessionBlocklistJpaService {
    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistJpaService.class);
    private final SessionBlocklistRepository sessionBlocklistRepository;
    private final VaultiqSessionRepository vaultiqSessionRepository;
    private final UserIdentityAware userIdentityAware;

    public SessionBlocklistJpaService(
            SessionBlocklistRepository sessionBlocklistRepository,
            VaultiqSessionRepository vaultiqSessionRepository,
            UserIdentityAware userIdentityAware
    ) {
        this.sessionBlocklistRepository = sessionBlocklistRepository;
        this.vaultiqSessionRepository = vaultiqSessionRepository;
        this.userIdentityAware = userIdentityAware;
    }

    /**
     * Blocklist (invalidate) sessions based on the provided context.
     * <p>
     *
     * @param context the context describing the blocklist operation
     */
    public void blocklist(BlocklistContext context) {
        if (context != null) {
            switch (context.getRevocationType()) {
                case LOGOUT -> blocklistSession(context.getIdentifier(), context.getNote());
                case LOGOUT_WITH_EXCLUSION ->
                        blocklistAllSessionsExcept(context.getIdentifier(), context.getNote(), context.getExcludedSessionIds());
                case LOGOUT_ALL -> blocklistAllSessions(context.getIdentifier(), context.getNote());
            }
        }
    }

    /**
     * Blocklist (invalidate) all sessions for a given user.
     * Can be used to log out from all devices.
     *
     * @param userId the user identifier
     */
    @Transactional
    public void blocklistAllSessions(String userId, String note) {
        List<JpaVaultiqSession> sessions = vaultiqSessionRepository.findAllByUserId(userId);
        var sessionBlocklist = sessions.stream()
                .map(session -> createBlocklist(note, session, RevocationType.LOGOUT_ALL))
                .toList();
        sessionBlocklistRepository.saveAll(sessionBlocklist);
        log.info("Blocklisted all sessions for user '{}'.", userId);
    }

    /**
     * Helper method to create a blocklist entity.
     * <p>
     *
     * @param note    the reason for the blocklist
     * @param session the session entity
     * @param type    the type of blocklist (e.g., LOGOUT, LOGOUT_WITH_EXCLUSION, LOGOUT_ALL)
     * @return the created blocklist entity
     */
    private SessionBlocklistEntity createBlocklist(String note, JpaVaultiqSession session, RevocationType type) {
        var blocklist = new SessionBlocklistEntity();
        blocklist.setSessionId(session.getSessionId());
        blocklist.setUserId(session.getUserId());
        blocklist.setRevocationType(type);
        blocklist.setNote(note);
        blocklist.setTriggeredBy(userIdentityAware.getCurrentUserID());
        blocklist.setBlocklistedAt(Instant.now());
        return blocklist;
    }

    /**
     * Blocklist (invalidate) all sessions except the specified session IDs.
     * Can be used to log out from all devices except, e.g., current device.
     *
     * @param userId             the user identifier
     * @param excludedSessionIds session IDs that should NOT be blocklisted
     */
    @Transactional
    public void blocklistAllSessionsExcept(String userId, String note, String... excludedSessionIds) {
        List<JpaVaultiqSession> sessions = vaultiqSessionRepository.findAllByUserId(userId);

        Set<String> excludedIds = excludedSessionIds == null
                ? Collections.emptySet()
                : new HashSet<>(Arrays.asList(excludedSessionIds));

        var sessionBlocklist = sessions.stream()
                .filter(session -> !excludedIds.contains(session.getSessionId()))
                .map(session -> createBlocklist(note, session, RevocationType.LOGOUT_WITH_EXCLUSION))
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
    public void blocklistSession(String sessionId, String note) {
        Optional<JpaVaultiqSession> optionalSession =
                vaultiqSessionRepository.findById(sessionId);
        if (optionalSession.isPresent()) {
            JpaVaultiqSession session = optionalSession.get();
            SessionBlocklistEntity entity = createBlocklist(note, session, RevocationType.LOGOUT);
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
    public boolean isSessionBlocklisted(String sessionId) {
        return sessionBlocklistRepository.existsById(sessionId);
    }

    /**
     * Get all blocklisted session IDs for a user.
     *
     * @param userId the user identifier
     * @return list of blocklisted session IDs for the user, or empty set if none
     */
    public List<SessionBlocklistEntity> getBlocklistedSessions(String userId) {
        return sessionBlocklistRepository.findAllByUserId(userId);
    }
}
