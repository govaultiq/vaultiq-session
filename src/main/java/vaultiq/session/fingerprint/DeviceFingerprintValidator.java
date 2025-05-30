package vaultiq.session.fingerprint;

import jakarta.servlet.http.HttpServletRequest;
import vaultiq.session.core.SessionManager;
import vaultiq.session.core.model.ClientSession;

/**
 * Functional interface for validating a session based on the device fingerprint.
 * <p>
 * This interface provides a contract that allows consuming applications to
 * customize how session authenticity is validated using device fingerprints.
 * The primary use case is to compare the fingerprint generated from the
 * current request with the fingerprint stored in the {@link ClientSession}
 * associated with the request.
 * </p>
 * <p>
 * Implementing and providing a custom bean of this type allows developers to
 * use their own validation logic. If no custom implementation is provided,
 * the library registers a default implementation (see
 * {@link DeviceFingerprintBeanConfigFallback}).
 * </p>
 *
 * @see DeviceFingerprintBeanConfigFallback
 * @see DeviceFingerprintGenerator
 * @see ClientSession#getDeviceFingerPrint()
 */
@FunctionalInterface
public interface DeviceFingerprintValidator {
    /**
     * Validates the session associated with the given request based on device fingerprint.
     * <p>
     * The implementation should typically:
     * <ol>
     * <li>Identify the session associated with the {@link HttpServletRequest} (e.g., using a session ID cookie or header).</li>
     * <li>Retrieve the corresponding {@link ClientSession} object (e.g., using {@link SessionManager}).</li>
     * <li>Generate a fingerprint from the current {@link HttpServletRequest} (e.g., using {@link DeviceFingerprintGenerator}).</li>
     * <li>Compare the generated fingerprint with the fingerprint stored in the retrieved {@link ClientSession}.</li>
     * <li>Return {@code true} if the fingerprints match and the session is considered valid for the device, {@code false} otherwise.</li>
     * </ol>
     * </p>
     *
     * @param request The incoming {@link HttpServletRequest}.
     * @return {@code true} if the session is valid for the device based on fingerprint, {@code false} otherwise.
     */
    boolean validateFingerprint(HttpServletRequest request);
}
