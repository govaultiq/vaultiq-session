package vaultiq.session.core;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import vaultiq.session.fingerprint.DeviceFingerprintValidator;

@Component
@ConditionalOnBean(VaultiqSessionManager.class)
public class VaultiqSessionValidator {
    private final VaultiqSessionManager sessionManager;
    private final DeviceFingerprintValidator deviceFingerprintValidator;

    public VaultiqSessionValidator(
            VaultiqSessionManager sessionManager,
            DeviceFingerprintValidator deviceFingerprintValidator) {
        this.sessionManager = sessionManager;
        this.deviceFingerprintValidator = deviceFingerprintValidator;
    }

    public boolean validateForSession(HttpServletRequest request) {
        // TODO: should validate if the sessionId (sid) is blocklisted
        return deviceFingerprintValidator.validateFingerprint(request);
    }
}
