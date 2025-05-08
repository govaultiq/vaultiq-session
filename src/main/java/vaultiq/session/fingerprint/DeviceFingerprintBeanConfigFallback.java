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
import vaultiq.session.utility.VaultiqRequestContext;

import java.util.Optional;

@Component
@ConditionalOnBean(VaultiqSessionManager.class)
public class DeviceFingerprintBeanConfigFallback {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintBeanConfigFallback.class);

    @Bean
    @ConditionalOnMissingBean
    DeviceFingerprintGenerator deviceFingerprintGenerator() {
        return request -> {
            String deviceId = safeHeader(request, "X-Device-Id");
            // orFallbackTo if X-Device-Id is not present
            deviceId = fallbackOrThrow(request, deviceId);

            String language = safeHeader(request, "Accept-Language");
            String platform = extractPlatform(request);

            var raw = deviceId + "|" + language + "|" + platform;
            return DigestUtils.sha256Hex(raw);
        };
    }

    private String fallbackOrThrow(HttpServletRequest request, String deviceId) {
        if (deviceId.equals(UNKNOWN))
            deviceId = safeHeader(request, "User-Agent");
        if (deviceId.equals(UNKNOWN))
            throw new IllegalArgumentException("Could not identify the client, X-Device-Id or User-Agent headers mandatory.");
        return deviceId;
    }

    @Bean
    @ConditionalOnMissingBean
    DeviceFingerprintValidator deviceFingerprintValidator(DeviceFingerprintGenerator fingerprintGenerator, VaultiqSessionManager sessionManager) {
        return (request) -> {
            var sessionId = VaultiqRequestContext.getSessionId(request);

            if (sessionId == null) {
                log.error("sessionId with key 'vaultiq.sid' is missing in the request.");
                return false;
            }

            var session = sessionManager.getSession(sessionId);

            if (session != null) {
                var fingerprint = fingerprintGenerator.generateFingerprint(request);
                return session.getDeviceFingerPrint().equals(fingerprint);
            } else {
                log.error("Could not find session for sessionId: {}", sessionId);
                return false;
            }
        };
    }

    private static final String UNKNOWN = "unknown";

    private String safeHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value != null ? value.trim().toLowerCase() : UNKNOWN;
    }

    private String extractPlatform(HttpServletRequest request) {
        String platform = request.getHeader("Sec-CH-UA-Platform");
        if (platform != null && !platform.isBlank()) {
            return platform.replace("\"", "").toLowerCase();
        }
        // Fallback
        return extractPlatformFromUserAgent(request.getHeader("User-Agent"));
    }

    private String extractPlatformFromUserAgent(String userAgent) {
        if (userAgent == null) return "unknown";
        if (userAgent.contains("Windows")) return "windows";
        if (userAgent.contains("Mac")) return "mac";
        if (userAgent.contains("X11")) return "unix";
        if (userAgent.contains("Android")) return "android";
        if (userAgent.contains("iPhone")) return "ios";
        return "other";
    }
}