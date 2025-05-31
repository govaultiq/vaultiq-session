package vaultiq.session.fingerprint;

import jakarta.servlet.http.HttpServletRequest;
import vaultiq.session.domain.model.ClientSession;

/**
 * Functional interface for generating a unique device fingerprint from an HTTP request.
 * <p>
 * This interface provides a contract that allows consuming applications to
 * customize how device fingerprints are generated. The library uses this
 * fingerprint to associate sessions with specific devices and potentially
 * validate session authenticity based on the device.
 * </p>
 * <p>
 * Implementing and providing a custom bean of this type allows developers to
 * use their own logic for fingerprint generation. If no custom implementation
 * is provided, the library registers a default implementation (see
 * {@link DeviceFingerprintBeanConfigFallback}).
 * </p>
 *
 * @see DeviceFingerprintBeanConfigFallback
 * @see ClientSession#getDeviceFingerPrint()
 */
@FunctionalInterface
public interface DeviceFingerprintGenerator {
    /**
     * Generates a unique string fingerprint for the device making the request.
     * <p>
     * The implementation should extract relevant information from the
     * {@link HttpServletRequest} (e.g., headers, client-hints) and combine
     * them into a consistent, unique identifier for the client device.
     * </p>
     *
     * @param request The incoming {@link HttpServletRequest}.
     * @return A unique string representing the device fingerprint. Must not be {@code null} or blank.
     */
    String generateFingerprint(HttpServletRequest request);
}
