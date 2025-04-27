package vaultiq.session.fingerprint;

import jakarta.servlet.http.HttpServletRequest;
import vaultiq.session.redis.model.VaultiqSession;

public interface DeviceFingerprintValidator {
    boolean validateFingerprint(HttpServletRequest request, VaultiqSession vaultiqSession);
}

