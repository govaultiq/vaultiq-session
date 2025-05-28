package vaultiq.session.core;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import vaultiq.session.cache.util.SessionIdRequestMapper;
import vaultiq.session.fingerprint.DeviceFingerprintValidator;

/**
 * Validates whether an incoming session request is legitimate.
 * <p>
 * This component performs two critical checks to determine the legitimacy
 * of a session associated with an incoming HTTP request:
 * <ul>
 *   <li><b>Blocklist Validation:</b> Ensures the session ID is not revoked.</li>
 *   <li><b>Device Fingerprint Validation:</b> Confirms the request's device characteristics
 *   match the original session's device fingerprint using {@link DeviceFingerprintValidator}.</li>
 * </ul>
 * <p>
 * This bean is only created when the application context includes all the following:
 * {@link VaultiqSessionManager}, {@link SessionRevocationManager}, and {@link DeviceFingerprintValidator}.
 * This ensures that session validation is only enabled when all required infrastructures are available.
 */
@Component
@ConditionalOnBean({VaultiqSessionManager.class, SessionRevocationManager.class, DeviceFingerprintValidator.class})
public class VaultiqSessionValidator {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionValidator.class);

    private final SessionRevocationManager revocationManager;
    private final DeviceFingerprintValidator deviceFingerprintValidator;

    public VaultiqSessionValidator(
            SessionRevocationManager revocationManager,
            DeviceFingerprintValidator deviceFingerprintValidator
    ) {
        this.revocationManager = revocationManager;
        this.deviceFingerprintValidator = deviceFingerprintValidator;
    }

    /**
     * Validates the session associated with the incoming HTTP request.
     * <p>
     * The method performs the following validations in sequence:
     * <ol>
     *   <li>Extracts the session ID from the request. If absent, validation fails.</li>
     *   <li>Checks whether the session ID is in the revoke. If revoked, validation fails.</li>
     *   <li>Validates the device fingerprint to ensure the request originates from the
     *   same device as the one that initiated the session.</li>
     * </ol>
     * <p>
     * If all checks pass, the session is considered valid.
     *
     * @param request the HTTP servlet request to validate
     * @return {@code true} if the session is legitimate and passes all checks; {@code false} otherwise
     */
    public boolean validateForSession(HttpServletRequest request) {
        String sessionId = SessionIdRequestMapper.getSessionId(request);

        if (sessionId == null) {
            log.warn("Session validation failed: No session ID found in the request.");
            return false;
        }

        if (revocationManager.isSessionRevoked(sessionId)) {
            log.warn("Session ID [{}] is revoked.", sessionId);
            return false;
        }

        boolean fingerprintValid = deviceFingerprintValidator.validateFingerprint(request);
        if (!fingerprintValid) {
            log.warn("Device fingerprint validation failed for session ID [{}].", sessionId);
            return false;
        }

        log.debug("Session ID [{}] passed all validation checks.", sessionId);
        return true;
    }
}
