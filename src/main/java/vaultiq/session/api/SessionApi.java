package vaultiq.session.api;

import jakarta.servlet.http.HttpServletRequest;
import vaultiq.session.model.ClientSession;
import vaultiq.session.model.RevocationRequest;

import java.util.List;

/**
 * API for managing client sessions in the system.
 * <p>
 * Provides methods for creating, validating, revoking, and retrieving session information.
 * Designed for typical user-session lifecycle operations like login, session check, and logout.
 */
public interface SessionApi {

    /**
     * Creates a new session for the specified user and device fingerprint.
     * This is typically invoked at login time.
     *
     * @param userId  the unique identifier of the user
     * @param request the HTTP request for fingerprinting
     * @return the newly created {@link ClientSession}
     */
    ClientSession createSession(String userId, HttpServletRequest request);

    /**
     * Retrieves all active sessions associated with the currently authenticated user.
     * Useful for displaying user's active sessions across devices.
     *
     * @return list of {@link ClientSession} instances active for the current user
     */
    List<ClientSession> getCurrentUserSessions();

    /**
     * Validates the incoming request by checking for a valid session.
     * This method extracts session context (typically from headers or cookies)
     * and verifies if the session is active and not revoked.
     *
     * @param request the incoming HTTP request
     * @return {@code true} if the session is valid, {@code false} otherwise
     */
    boolean validate(HttpServletRequest request);

    /**
     * Revokes an existing session. Commonly used during logout or security events.
     *
     * @param request the revocation request containing session identifier(s) and optional metadata
     */
    void revoke(RevocationRequest request);
}
