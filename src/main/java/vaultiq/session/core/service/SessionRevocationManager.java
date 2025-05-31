package vaultiq.session.core.service;

import vaultiq.session.model.RevokedSession;
import vaultiq.session.model.RevocationRequest;

import java.util.List;

/**
 * Manages the revocation (invalidating) of Vaultiq sessions.
 * <p>
 * Defines the contract for services responsible for marking sessions
 * as revoked to prevent further use. Implementations manage storage and retrieval
 * of revoked session data according to configured persistence strategies.
 * </p>
 * <p>
 * Consumers should rely on this interface without concerning themselves
 * with the underlying persistence mechanism.
 * </p>
 */
public interface SessionRevocationManager {

    /**
     * Revokes one or more sessions based on the given revocation request.
     * <p>
     * The specific sessions and revocation rationale come from {@link RevocationRequest}.
     * Implementations persist this data per their configured strategy.
     * </p>
     *
     * @param revocationRequest the revocation request; must not be null.
     */
    void revoke(RevocationRequest revocationRequest);

    /**
     * Checks if a session is currently revoked.
     * <p>
     * Queries the underlying persistence to verify revocation status.
     * </p>
     *
     * @param sessionId the unique session identifier; must not be blank.
     * @return true if the session is revoked, false otherwise.
     */
    boolean isSessionRevoked(String sessionId);

    /**
     * Retrieves all revoked sessions for a specific user.
     * <p>
     * Provides details about each revoked session.
     * </p>
     *
     * @param userId the user identifier; must not be blank.
     * @return a list of revoked sessions; never null but may be empty.
     */
    List<RevokedSession> getRevokedSessions(String userId);
}
