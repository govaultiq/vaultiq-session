package vaultiq.session.fingerprint;

import jakarta.servlet.http.HttpServletRequest;

public interface DeviceFingerprintValidator {
    boolean validateFingerprint(HttpServletRequest request);
}

