
package vaultiq.session.jpa.revoke.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vaultiq.session.domain.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.domain.contracts.internal.SessionRevocationManager;
import vaultiq.session.domain.model.RevokedSession;
import vaultiq.session.jpa.revoke.model.RevokedSessionEntity;
import vaultiq.session.jpa.revoke.service.internal.RevokedSessionEntityService;
import vaultiq.session.domain.model.RevocationRequest;

import java.util.*;

/**
 * JPA-backed implementation of the {@link SessionRevocationManager} interface.
 * <p>
 * This service delegates all session revoking operations for the REVOKE model type
 * to {@link RevokedSessionEntityService}. revoking functionality includes invalidating
 * all sessions, all-except-some, specific sessions, and querying revoked state by session ID
 * or collecting all revoked session IDs for a user.
 * </p>
 * <p>
 * This bean is activated only if a {@code RevokedSessionEntityService} bean is present in the context,
 * and the persistence configuration matches JPA mode with a REVOKE model type.
 * </p>
 *
 * @see SessionRevocationManager
 * @see RevokedSessionEntityService
 */
@Service
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_ONLY, type = ModelType.REVOKE)
public class SessionRevocationManagerViaJpa implements SessionRevocationManager {

    private static final Logger log = LoggerFactory.getLogger(SessionRevocationManagerViaJpa.class);

    private final RevokedSessionEntityService revokedSessionEntityService;

    /**
     * Constructs a new {@code SessionRevocationManagerViaJpa} with the required JPA service dependency.
     *
     * @param revokedSessionEntityService the underlying service that actually performs JPA-based operations
     */
    public SessionRevocationManagerViaJpa(RevokedSessionEntityService revokedSessionEntityService) {
        this.revokedSessionEntityService = revokedSessionEntityService;
        log.info("SessionRevocationManager initialized; Persistence via - JPA_ONLY.");
    }

    /**
     * Revoke (invalidate) sessions based on the provided context.
     *
     * @param context the context describing the revoke operation
     */
    @Override
    public void revoke(RevocationRequest context) {
        revokedSessionEntityService.revoke(context);
    }

    /**
     * Determines if a given session is currently revoked.
     *
     * @param sessionId the session ID to check
     * @return {@code true} if the session is revoked, {@code false} otherwise
     */
    @Override
    public boolean isSessionRevoked(String sessionId) {
        log.debug("Checking if session '{}' is revoked.", sessionId);
        return revokedSessionEntityService.isSessionRevoked(sessionId);
    }

    /**
     * Retrieves all revoked session IDs for the specified user, or an empty set if none.
     *
     * @param userId the user identifier for which to retrieve revoked sessions
     * @return a list of revoked sessions, never {@code null}
     */
    @Override
    public List<RevokedSession> getRevokedSessions(String userId) {
        log.debug("Fetching revoked sessions for user '{}'.", userId);
        return revokedSessionEntityService.getRevokedSessions(userId)
                .stream()
                .map(this::toSessionRevoke)
                .toList();
    }

    /**
     * Helper method to convert a JPA entity to a RevokedSession object.
     *
     * @param revokedSessionEntity the JPA entity to convert
     * @return the converted RevokedSession object
     */
    private RevokedSession toSessionRevoke(RevokedSessionEntity revokedSessionEntity) {
        return RevokedSession.builder()
                .sessionId(revokedSessionEntity.getSessionId())
                .userId(revokedSessionEntity.getUserId())
                .revocationType(revokedSessionEntity.getRevocationType())
                .note(revokedSessionEntity.getNote())
                .triggeredBy(revokedSessionEntity.getTriggeredBy())
                .revokedAt(revokedSessionEntity.getRevokedAt())
                .build();
    }
}
