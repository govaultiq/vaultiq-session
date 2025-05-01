package vaultiq.session.fingerprint;

import jakarta.servlet.http.HttpServletRequest;
import vaultiq.session.cache.model.VaultiqSessionCacheEntry;

public interface DeviceFingerprintValidator {
    boolean validateFingerprint(HttpServletRequest request, VaultiqSessionCacheEntry vaultiqSession);
}

