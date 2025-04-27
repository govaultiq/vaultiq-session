package vaultiq.session.fingerprint;

import jakarta.servlet.http.HttpServletRequest;
import vaultiq.session.redis.model.RedisVaultiqSession;

public interface DeviceFingerprintValidator {
    boolean validateFingerprint(HttpServletRequest request, RedisVaultiqSession vaultiqSession);
}

