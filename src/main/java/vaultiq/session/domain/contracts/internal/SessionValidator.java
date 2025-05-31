package vaultiq.session.domain.contracts.internal;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Validates whether an incoming session request is legitimate.
 * <p>
 * This component performs two critical checks to determine the legitimacy
 * of a session associated with an incoming HTTP request.
 * </p>
 */
public interface SessionValidator {

    /**
     * Validate request for session validity. Ensuring the session is non-revoked and device fingerprint matches.
     * <p>
     * @param request the HTTP servlet request to validate
     * @return {@code true} if the session is legitimate and passes all checks; {@code false} otherwise
     */
    boolean validateForSession(HttpServletRequest request);
}
