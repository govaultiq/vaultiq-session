
package vaultiq.session.jpa.blocklist.service.internal;

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
import vaultiq.session.jpa.session.model.VaultiqSessionEntity;
import vaultiq.session.jpa.blocklist.model.SessionBlocklistEntity;
import vaultiq.session.jpa.blocklist.repository.SessionBlocklistEntityRepository;
import vaultiq.session.jpa.session.repository.VaultiqSessionEntityRepository;
import vaultiq.session.core.util.BlocklistContext;

import java.time.Instant;
import java.util.*;

/**
 * Service for blocklisting (invalidating) user sessions using the JPA (database) persistence strategy.
 * <p>
 * Provides methods to blocklist individual sessions, all sessions for a user, or all except some for a user.
 * Methods prefer transactional semantics for batch operations. The database is the source of truth in this persistence mode.
 * </p>
 *
 * Typical usage includes logging-out sessions, forced logout for security, and checking blocklist status for token validation.
 */
@Service
@ConditionalOnBean(UserIdentityAware.class)
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_JPA, type = ModelType.BLOCKLIST)
public class SessionBlocklistEntityService {
    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistEntityService.class);

    private final SessionBlocklistEntityRepository sessionBlocklistEntityRepository;
    private final VaultiqSessionEntityRepository vaultiqSessionEntityRepository;
    private final UserIdentityAware userIdentityAware;

    /**
     * Constructs a JPA-backed blocklist service for sessions.
     *
     * @param sessionBlocklistEntityRepository repository for blocklist entities
     * @param vaultiqSessionEntityRepository   repository for session entities
     * @param userIdentityAware          provides the current user for audit logging
     */
    public SessionBlocklistEntityService(
            SessionBlocklistEntityRepository sessionBlocklistEntityRepository,
            VaultiqSessionEntityRepository vaultiqSessionEntityRepository,
            UserIdentityAware userIdentityAware
    ) {
        this.sessionBlocklistEntityRepository = sessionBlocklistEntityRepository;
        this.vaultiqSessionEntityRepository = vaultiqSessionEntityRepository;
        this.userIdentityAware = userIdentityAware;
    }

    /**
     * Blocklist (invalidate) sessions based on the provided revocation context.
     * <p>
     * The action may include blocking a single session, all sessions, or all-but-some sessions for a user.
     * </p>
     *
     * @param context the context describing the blocklist operation and strategy
     */
    @Transactional
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
     * Blocklists (invalidates) all sessions for the specified user.
     * Typically used to force logout from all devices.
     *
     * @param userId the user identifier
     * @param note   optional reason or audit note for the operation
     */
    @Transactional
    public void blocklistAllSessions(String userId, String note) {
        List<VaultiqSessionEntity> sessions = vaultiqSessionEntityRepository.findAllByUserId(userId);
        var sessionBlocklist = sessions.stream()
                .map(session -> createBlocklist(note, session, RevocationType.LOGOUT_ALL))
                .toList();
        sessionBlocklistEntityRepository.saveAll(sessionBlocklist);
        log.info("Blocklisted all sessions for user '{}'.", userId);
    }

    /**
     * Helper method to construct a blocklist entity from a raw session, blocklist type, and note.
     *
     * @param note    the reason for the blocklist (may be null)
     * @param session the session entity to blocklist
     * @param type    the type of blocklist operation
     * @return the newly created blocklist entity object (not persisted)
     */
    private SessionBlocklistEntity createBlocklist(String note, VaultiqSessionEntity session, RevocationType type) {
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
     * Blocklists (invalidates) all sessions for a user except the specified session IDs.
     * Useful for "logout everywhere else" features.
     *
     * @param userId             the user whose sessions are to be mostly blocklisted
     * @param note               optional reason or audit note
     * @param excludedSessionIds session IDs to be excluded from blocklisting (may be null/empty)
     */
    @Transactional
    public void blocklistAllSessionsExcept(String userId, String note, String... excludedSessionIds) {
        List<VaultiqSessionEntity> sessions = vaultiqSessionEntityRepository.findAllByUserId(userId);

        Set<String> excludedIds = excludedSessionIds == null
                ? Collections.emptySet()
                : new HashSet<>(Arrays.asList(excludedSessionIds));

        var sessionBlocklist = sessions.stream()
                .filter(session -> !excludedIds.contains(session.getSessionId()))
                .map(session -> createBlocklist(note, session, RevocationType.LOGOUT_WITH_EXCLUSION))
                .toList();

        sessionBlocklistEntityRepository.saveAll(sessionBlocklist);
        log.info("Blocklisted all sessions for user '{}' except {}.", userId, excludedIds);
    }

    /**
     * Blocklists (invalidates) a single session by session ID.
     * Fails gracefully if the session does not exist.
     *
     * @param sessionId the session identifier to blocklist
     * @param note      optional note for auditing purposes
     * @return the blocklist entity if blocklisting was successful, null if session was missing
     */
    @Transactional
    public SessionBlocklistEntity blocklistSession(String sessionId, String note) {
        Optional<VaultiqSessionEntity> optionalSession =
                vaultiqSessionEntityRepository.findById(sessionId);
        if (optionalSession.isPresent()) {
            VaultiqSessionEntity session = optionalSession.get();
            SessionBlocklistEntity entity = createBlocklist(note, session, RevocationType.LOGOUT);
            log.info("Blocklisted session '{}'.", sessionId);
            return sessionBlocklistEntityRepository.save(entity);
        } else {
            log.warn("Attempted to blocklist non-existent session '{}'.", sessionId);
            return null;
        }
    }

    /**
     * Checks if a session is currently blocklisted.
     *
     * @param sessionId the session identifier to check
     * @return true if the session is blocklisted, false otherwise
     */
    public boolean isSessionBlocklisted(String sessionId) {
        return sessionBlocklistEntityRepository.existsById(sessionId);
    }

    /**
     * Retrieves all blocklisted session entities for a given user.
     *
     * @param userId the user identifier
     * @return list of blocklisted session entities for the user, or empty list if none
     */
    public List<SessionBlocklistEntity> getBlocklistedSessions(String userId) {
        return sessionBlocklistEntityRepository.findAllByUserId(userId);
    }

    /**
     * Retrieves a single blocklisted session entity by session ID.
     *
     * @param sessionId the session ID to fetch
     * @return the blocklist entity if found, otherwise null
     */
    public SessionBlocklistEntity getBlocklistedSession(String sessionId) {
        return sessionBlocklistEntityRepository.findById(sessionId).orElse(null);
    }

    /**
     * Determines if any blocklist entries for the user have been updated after a specified timestamp.
     * Typically used for cache staleness checks.
     *
     * @param userId        the user whose sessions to check
     * @param lastUpdatedAt the cutoff timestamp (epoch millis)
     * @return true if a more recent blocklist entry exists, otherwise false
     */
    public boolean isLastUpdatedGreaterThan(String userId, Long lastUpdatedAt) {
        return sessionBlocklistEntityRepository.existsByUserIdAndBlocklistedAtGreaterThan(userId, Instant.ofEpochMilli(lastUpdatedAt));
    }
}
