package vaultiq.session.fingerprint;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.cache.util.SessionIdRequestMapper;
import vaultiq.session.core.model.VaultiqSession;

/**
 * Provides default bean configurations for {@link DeviceFingerprintGenerator}
 * and {@link DeviceFingerprintValidator}.
 * <p>
 * This class is automatically configured by Spring Boot when the
 * {@code vaultiq-session} library is used, if no custom beans
 * of {@link DeviceFingerprintGenerator} or {@link DeviceFingerprintValidator}
 * are already present in the application context. It acts as a fallback
 * to ensure basic device fingerprinting and validation functionality is
 * available out-of-the-box.
 * </p>
 * <p>
 * <b>Recommendation:</b> It is generally recommended to rely on the default
 * implementations provided by this class unless there is a strong requirement
 * for highly specific or advanced fingerprinting logic. The default
 * implementation is designed to be reasonably robust and efficient for common
 * web application scenarios by leveraging standard HTTP headers and hashing.
 * Custom implementations should be carefully designed and tested to ensure
 * consistency, collision avoidance, and the appropriate handling of various client
 * requests.
 * </p>
 */
@Component
@ConditionalOnBean(VaultiqSessionManager.class)
public class DeviceFingerprintBeanConfigFallback {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintBeanConfigFallback.class);

    private static final String UNKNOWN = "unknown"; // Constant used for unknown or missing header values

    /**
     * Configures a default {@link DeviceFingerprintGenerator} bean if no other
     * bean of this type is found in the Spring application context.
     * <p>
     * The default generator creates a fingerprint by combining several request
     * attributes:
     * <ul>
     * <li>A primary device identifier (prefers {@code X-Device-Id}, falls back to {@code User-Agent}).</li>
     * <li>The {@code Accept-Language} header.</li>
     * <li>The client platform (derived from {@code Sec-CH-UA-Platform} or {@code User-Agent}).</li>
     * </ul>
     * The combined string is then hashed using SHA-256 to produce a consistent fingerprint.
     * </p>
     * <p>
     * An {@link IllegalArgumentException} is thrown during fingerprint generation
     * if neither the {@code X-Device-Id} nor the {@code User-Agent} headers are
     * present in the request, as a primary device identifier is mandatory.
     * </p>
     *
     * @return A default implementation of {@link DeviceFingerprintGenerator}.
     */
    @Bean
    @ConditionalOnMissingBean
    DeviceFingerprintGenerator deviceFingerprintGenerator() {
        return request -> {
            // Attempt to get device ID from X-Device-Id header, falling back to User-Agent.
            String deviceId = safeHeader(request, "X-Device-Id");
            deviceId = fallbackOrThrow(request, deviceId);

            String language = safeHeader(request, "Accept-Language");
            String platform = extractPlatform(request);

            // Combine the raw components into a single string.
            var raw = deviceId + "|" + language + "|" + platform;
            return DigestUtils.sha256Hex(raw);
        };
    }

    /**
     * Configures a default {@link DeviceFingerprintValidator} bean if no other
     * bean of this type is found in the Spring application context.
     * <p>
     * The default validator checks if the fingerprint generated from the current
     * request matches the fingerprint stored in the {@link VaultiqSession}
     * associated with the request's session ID. This helps verify that the
     * session is being used from the same device it was created on.
     * </p>
     * <p>
     * Requires a {@link DeviceFingerprintGenerator} (to generate the current
     * request's fingerprint) and a {@link VaultiqSessionManager} (to retrieve
     * the stored session) to be available as Spring beans.
     * </p>
     *
     * @param fingerprintGenerator The {@link DeviceFingerprintGenerator} bean.
     * @param sessionManager       The {@link VaultiqSessionManager} bean.
     * @return A default implementation of {@link DeviceFingerprintValidator}.
     */
    @Bean
    @ConditionalOnMissingBean
    DeviceFingerprintValidator deviceFingerprintValidator(DeviceFingerprintGenerator fingerprintGenerator, VaultiqSessionManager sessionManager) {
        return (request) -> {
            var sessionId = SessionIdRequestMapper.getSessionId(request);

            if (sessionId == null) {
                log.error("sessionId with key '"+SessionIdRequestMapper.VAULTIQ_SID_KEY+"' is missing in the request.");
                return false;
            }

            var session = sessionManager.getSession(sessionId);

            if (session != null) {

                if(session.isBlocked()) {
                    log.error("Session ID [{}] is blocked. Fingerprint validation aborted.", sessionId);
                    return false;
                }

                var currentFingerprint = fingerprintGenerator.generateFingerprint(request);
                var isValid = session.getDeviceFingerPrint().equals(currentFingerprint);

                if(!isValid)
                    log.error("Device fingerprint mismatch for session ID [{}].", sessionId);

                return isValid;

            } else {
                log.error("Could not find session for sessionId: {}", sessionId);
                return false;
            }
        };
    }

    /**
     * Provides a fallback mechanism for the primary device identifier.
     * <p>
     * If the initially retrieved {@code deviceId} is {@code UNKNOWN}, it attempts
     * to use the {@code User-Agent} header as a fallback. If both are {@code UNKNOWN},
     * it throws an {@link IllegalArgumentException} indicating that a mandatory
     * client identifier could not be determined.
     * </p>
     *
     * @param request  The current HTTP request.
     * @param deviceId The device ID obtained from the primary source (e.g., {@code X-Device-Id}).
     * @return The resolved device ID, either from the primary source or the {@code User-Agent} fallback.
     * @throws IllegalArgumentException if a mandatory device identifier cannot be found.
     */
    private String fallbackOrThrow(HttpServletRequest request, String deviceId) {
        if (deviceId.equals(UNKNOWN)) {
            deviceId = safeHeader(request, "User-Agent");
        }
        if (deviceId.equals(UNKNOWN)) {
            throw new IllegalArgumentException("Could not identify the client, X-Device-Id or User-Agent headers mandatory.");
        }
        return deviceId;
    }

    /**
     * Safely retrieves a header value from the {@link HttpServletRequest}.
     * <p>
     * Returns the header value if present, trimmed, and converted to lowercase.
     * If the header is missing or blank, returns the {@code UNKNOWN} constant.
     * </p>
     *
     * @param request The current HTTP request.
     * @param name    The name of the header to retrieve.
     * @return The header value in lowercase, or {@code UNKNOWN} if not found or blank.
     */
    private String safeHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value != null && !value.isBlank() ? value.trim().toLowerCase() : UNKNOWN;
    }

    /**
     * Extracts platform information from the request headers.
     * <p>
     * Prioritizes the {@code Sec-CH-UA-Platform} client hint header. If not
     * available or blank, it falls back to attempting to extract platform
     * information by parsing the {@code User-Agent} header string.
     * </p>
     *
     * @param request The current HTTP request.
     * @return A lowercase string representing the client platform (e.g., "windows", "mac", "android", "ios", "unix"),
     * or "other" if the platform cannot be determined from the available headers.
     */
    private String extractPlatform(HttpServletRequest request) {
        String platform = request.getHeader("Sec-CH-UA-Platform");
        if (platform != null && !platform.isBlank()) {
            return platform.replace("\"", "").toLowerCase();
        }
        // If a client hint is not available, fallback to parsing the User-Agent header.
        return extractPlatformFromUserAgent(request.getHeader("User-Agent"));
    }

    /**
     * Attempts to extract platform information by parsing the User-Agent string.
     * <p>
     * This method checks for common platform keywords within the User-Agent string.
     * </p>
     *
     * @param userAgent The User-Agent header string (can be {@code null}).
     * @return A lowercase string representing the identified platform (e.g., "windows", "mac", "android", "ios", "unix"),
     * or "other" if the platform cannot be recognized from the string. Returns {@code UNKNOWN} if the User-Agent string is null.
     */
    private String extractPlatformFromUserAgent(String userAgent) {
        if (userAgent == null) return UNKNOWN;
        if (userAgent.contains("Windows")) return "windows";
        if (userAgent.contains("Mac")) return "mac";
        if (userAgent.contains("X11")) return "unix";
        if (userAgent.contains("Android")) return "android";
        if (userAgent.contains("iPhone")) return "ios";
        return "other";
    }
}
