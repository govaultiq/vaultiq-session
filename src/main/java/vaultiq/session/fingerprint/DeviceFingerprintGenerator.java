package vaultiq.session.fingerprint;

import jakarta.servlet.http.HttpServletRequest;

public interface DeviceFingerprintGenerator {
    String generateFingerprint(HttpServletRequest request);
}
