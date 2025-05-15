package vaultiq.session.core;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import vaultiq.session.cache.util.SessionIdRequestMapper;
import vaultiq.session.fingerprint.DeviceFingerprintValidator;

/**
 * Central component responsible for validating whether a session request
 * is allowed to proceed based on session blocklist status and device fingerprint validation.
 *
 * <p>This validator checks the session ID (extracted from the request)
 * against the blocklist manager and also verifies the request's device
 * fingerprint through {@link DeviceFingerprintValidator}.
 *
 * <p>Bean is conditionally loaded only when core session management components
 * are available: {@link VaultiqSessionManager}, {@link SessionBacklistManager},
 * and {@link DeviceFingerprintValidator}.
 */
@Component
@ConditionalOnBean({VaultiqSessionManager.class, SessionBacklistManager.class, DeviceFingerprintValidator.class})
public class VaultiqSessionValidator {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionValidator.class);

    private final SessionBacklistManager blocklistManager;
    private final DeviceFingerprintValidator deviceFingerprintValidator;

    public VaultiqSessionValidator(
            SessionBacklistManager blocklistManager,
            DeviceFingerprintValidator deviceFingerprintValidator
    ) {
        this.blocklistManager = blocklistManager;
        this.deviceFingerprintValidator = deviceFingerprintValidator;
    }

    /**
     * Validates the incoming HTTP request for session legitimacy.
     *
     * @param request the incoming HTTP servlet request
     * @return {@code true} if session is valid and passes fingerprint check;
     *         {@code false} otherwise.
     */
    public boolean validateForSession(HttpServletRequest request) {
        String sessionId = SessionIdRequestMapper.getSessionId(request);

        if (sessionId == null) {
            log.warn("Session validation failed: No session ID found in the request.");
            return false;
        }

        if (blocklistManager.isSessionBlocklisted(sessionId)) {
            log.warn("Session ID [{}] is blocklisted.", sessionId);
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
