package vaultiq.session.jpa.revoke.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vaultiq.session.core.util.BatchProcessingHelper;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.core.spi.UserIdentityAware;
import vaultiq.session.jpa.revoke.model.RevokedSessionEntity;
import vaultiq.session.jpa.revoke.repository.RevokedSessionEntityRepository;
import vaultiq.session.jpa.session.model.ClientSessionEntity;
import vaultiq.session.jpa.session.repository.ClientSessionEntityRepository;
import vaultiq.session.model.ModelType;
import vaultiq.session.model.RevocationRequest;
import vaultiq.session.model.RevocationType;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * JPA-backed service for revoking (invalidating) user sessions.
 * <p>
 * Supports revoking individual sessions, all sessions for a user,
 * or all except specified ones. Uses transactional semantics for batch operations.
 * The database is the source of truth when JPA is enabled.
 * </p>
 */
@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_JPA, type = ModelType.REVOKE)
public class RevokedSessionEntityService {
    private static final Logger log = LoggerFactory.getLogger(RevokedSessionEntityService.class);

    private final RevokedSessionEntityRepository revokedSessionRepo;
    private final ClientSessionEntityRepository sessionRepo;
    private final UserIdentityAware userIdentityAware;

    public RevokedSessionEntityService(
            RevokedSessionEntityRepository revokedSessionRepo,
            ClientSessionEntityRepository sessionRepo,
            UserIdentityAware userIdentityAware
    ) {
        this.revokedSessionRepo = revokedSessionRepo;
        this.sessionRepo = sessionRepo;
        this.userIdentityAware = userIdentityAware;
    }

    /**
     * Processes a revocation request, handling different revocation types.
     *
     * @param request the revocation request; may revoke one, all, or all-except.
     */
    @Transactional
    public void revoke(RevocationRequest request) {
        if (request == null) {
            log.warn("Revoke Attempted; Nothing to revoke - request was null.");
            return;
        }

        RevocationType type = request.getRevocationType();
        String note = request.getNote();

        String id = switch (type) {
            case LOGOUT ->
                // session revoke must have a sessionId
                    Objects.requireNonNull(request.getIdentifier(),
                            "sessionId is required for LOGOUT");
            case LOGOUT_ALL, LOGOUT_WITH_EXCLUSION ->
                // user-level revoke can fall back to current user
                    request.isForCurrentUser()
                            ? userIdentityAware.getCurrentUserID()
                            : request.getIdentifier();
        };

        switch (type) {
            case LOGOUT -> revokeSession(id, note);
            case LOGOUT_ALL -> revokeAll(id, note);
            case LOGOUT_WITH_EXCLUSION -> revokeAllExcept(id, note, request.getExcludedSessionIds());
        }
    }


    /**
     * Revokes all sessions for a user.
     *
     * @param userId the user identifier
     * @param note   optional audit note
     */
    @Transactional
    public void revokeAll(String userId, String note) {
        var sessions = sessionRepo.findAllByUserId(userId);
        var entities = sessions.stream()
                .map(s -> toEntity(s, note, RevocationType.LOGOUT_ALL))
                .toList();
        revokedSessionRepo.saveAll(entities);
        log.info("Revoked all sessions for user '{}'.", userId);
    }

    /**
     * Revokes all sessions for a user except specified ones.
     *
     * @param userId      the user identifier
     * @param note        optional audit note
     * @param excludedIds session IDs to exclude
     */
    @Transactional
    public void revokeAllExcept(String userId, String note, Set<String> excludedIds) {
        var sessions = sessionRepo.findAllByUserIdAndIsRevokedFalseAndSessionIdNotIn(userId, excludedIds);
        var entities = sessions.stream()
                .map(s -> toEntity(s, note, RevocationType.LOGOUT_WITH_EXCLUSION))
                .toList();
        revokedSessionRepo.saveAll(entities);
        log.info("Revoked {} sessions for user '{}' except {}.", entities.size(), userId, excludedIds);
    }

    /**
     * Revokes a single session by ID.
     *
     * @param sessionId the session identifier
     * @param note      optional audit note
     * @return the persisted entity, or null if not found
     */
    @Transactional
    public RevokedSessionEntity revokeSession(String sessionId, String note) {
        return sessionRepo.findById(sessionId)
                .map(s -> {
                    var e = toEntity(s, note, RevocationType.LOGOUT);
                    log.info("Revoked session '{}'.", sessionId);
                    return revokedSessionRepo.save(e);
                })
                .orElseGet(() -> {
                    log.warn("Attempted to revoke non-existent session '{}'.", sessionId);
                    return null;
                });
    }

    /**
     * Checks if a session is revoked.
     *
     * @param sessionId the session identifier
     * @return true if revoked, false otherwise
     */
    public boolean isSessionRevoked(String sessionId) {
        return revokedSessionRepo.existsById(sessionId);
    }

    /**
     * Retrieves all revoked sessions for a user.
     *
     * @param userId the user identifier
     * @return list of revoked session entities; empty if none
     */
    public List<RevokedSessionEntity> getRevokedSessions(String userId) {
        return revokedSessionRepo.findAllByUserId(userId);
    }

    /**
     * Retrieves a revoked session by ID.
     *
     * @param sessionId the session identifier
     * @return the entity or null if not found
     */
    public RevokedSessionEntity getRevokedSession(String sessionId) {
        return revokedSessionRepo.findById(sessionId).orElse(null);
    }

    /**
     * Checks if any revocation entries for a user occurred after the given timestamp.
     *
     * @param userId        the user identifier
     * @param lastUpdatedMs cutoff timestamp in millis
     * @return true if newer entries exist, false otherwise
     */
    public boolean isLastUpdatedAfter(String userId, long lastUpdatedMs) {
        return revokedSessionRepo.existsByUserIdAndRevokedAtGreaterThan(
                userId, Instant.ofEpochMilli(lastUpdatedMs)
        );
    }

    /**
     * Clears revocation for specified sessions.
     *
     * @param sessionIds session IDs to clear
     */
    public void clearRevocation(String... sessionIds) {
        var ids = sessionIds == null || sessionIds.length == 0
                ? Collections.<String>emptySet()
                : new HashSet<>(Arrays.asList(sessionIds));
        if (!ids.isEmpty()) revokedSessionRepo.deleteAllById(ids);
        log.info("Cleared revocation for sessions: {}", ids);
    }

    /**
     * Deletes all revoked sessions older than a specified retention period.
     * Uses batch processing to avoid memory issues with large datasets.
     *
     * @param retentionPeriod the retention period
     * @return the number of records deleted
     */
    @Transactional
    public int deleteAllRevokedSessions(Duration retentionPeriod) {
        if (retentionPeriod == null || retentionPeriod.isNegative() || retentionPeriod.isZero()) {
            log.warn("Invalid retention period for cleanup: {}", retentionPeriod);
            return 0;
        }
        
        Instant cutoffTime = BatchProcessingHelper.calculateCutoffTime(retentionPeriod);
        log.debug("Starting batch deletion of revoked sessions older than {}", cutoffTime);
        
        return BatchProcessingHelper.batchDelete(
                pageable -> revokedSessionRepo.findByRevokedAtBefore(cutoffTime, pageable),
                revokedSessionRepo,
                "revoked sessions"
        );
    }


    /**
     * Helper to create a RevokedSessionEntity from a ClientSessionEntity.
     */
    private RevokedSessionEntity toEntity(ClientSessionEntity s, String note, RevocationType type) {
        var e = new RevokedSessionEntity();
        e.setSessionId(s.getSessionId());
        e.setUserId(s.getUserId());
        e.setRevocationType(type);
        e.setNote(note);
        e.setTriggeredBy(userIdentityAware.getCurrentUserID());
        e.setRevokedAt(Instant.now());
        return e;
    }
}