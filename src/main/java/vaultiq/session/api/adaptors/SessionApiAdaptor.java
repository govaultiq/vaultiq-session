package vaultiq.session.api.adaptors;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.api.SessionApi;
import vaultiq.session.core.service.SessionManager;
import vaultiq.session.core.service.SessionRevocationManager;
import vaultiq.session.core.service.SessionValidator;
import vaultiq.session.core.spi.UserIdentityAware;
import vaultiq.session.model.ClientSession;
import vaultiq.session.model.RevocationRequest;

import java.util.List;

/**
 * Adaptor implementation of {@link vaultiq.session.api.SessionApi} facade that delegate session operations
 * to the core session management, validation, and revocation services.
 * <p>
 * This service is only loaded if all required session-related beans are present in the application context.
 */
@Service
@ConditionalOnBean({SessionManager.class, SessionRevocationManager.class, SessionValidator.class, UserIdentityAware.class})
public class SessionApiAdaptor implements SessionApi {

    private final SessionManager sessionManager;
    private final SessionRevocationManager sessionRevocationManager;
    private final SessionValidator sessionValidator;
    private final UserIdentityAware userIdAware;

    public SessionApiAdaptor(
            SessionManager sessionManager,
            SessionRevocationManager sessionRevocationManager,
            SessionValidator sessionValidator,
            UserIdentityAware userIdAware
    ) {
        this.sessionManager = sessionManager;
        this.sessionRevocationManager = sessionRevocationManager;
        this.sessionValidator = sessionValidator;
        this.userIdAware = userIdAware;
    }

    /**
     * @param userId  the ID of the user for whom the session is to be created
     * @param request the HTTP servlet request for fingerprinting
     * @return the created {@link ClientSession}
     * @inheritedDoc initiates session creation via the {@link SessionManager}.
     */
    @Override
    public ClientSession createSession(String userId, HttpServletRequest request) {
        return sessionManager.createSession(userId, request);
    }

    /**
     * @return a list of {@link ClientSession} objects for the current user
     * @inheritedDoc fetches the active user sessions via the {@link SessionManager}.
     */
    @Override
    public List<ClientSession> getCurrentUserSessions() {
        return sessionManager.getActiveSessionsByUser(userIdAware.getCurrentUserID());
    }

    /**
     * @param request the HTTP servlet request to validate
     * @return {@code true} if the session is valid, {@code false} otherwise
     * @inheritedDoc initiates session validation via the {@link SessionValidator}.
     */
    @Override
    public boolean validate(HttpServletRequest request) {
        return sessionValidator.validateForSession(request);
    }

    /**
     * @param request the revocation request containing session revocation details
     * @inheritedDoc initiates revocation via the {@link SessionRevocationManager}.
     */
    @Override
    public void revoke(RevocationRequest request) {
        sessionRevocationManager.revoke(request);
    }
}
