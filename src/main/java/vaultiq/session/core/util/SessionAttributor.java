package vaultiq.session.core.util;

import jakarta.servlet.http.HttpServletRequest;
import vaultiq.session.model.DeviceType;

import java.util.Optional;

/**
 * Utility class for extracting and mapping session-related attributes from an {@link HttpServletRequest}.
 * <p>
 * Provides a convenient way to auto-resolve common session and device fields from incoming requests.
 * </p>
 */
public class SessionAttributor {
    /** Attribute key for client session ID. */
    public static final String CLIENT_SESSION_ID_KEY = "client-session-id";
    /** Header key for device name. */
    public static final String DEVICE_NAME = "device-name";
    /** Header key for operating system. */
    public static final String OS = "os";
    /** Header key for device type. */
    public static final String DEVICE_TYPE = "device-type";

    private final String sessionId;
    private final String deviceName;
    private final String os;
    private final DeviceType deviceType;

    /**
     * Constructs a new SessionAttributor with the given values.
     * @param sessionId the session ID
     * @param deviceName the device name
     * @param os the operating system
     * @param deviceType the device type
     */
    public SessionAttributor(String sessionId, String deviceName, String os, DeviceType deviceType) {
        this.sessionId = sessionId;
        this.deviceName = deviceName;
        this.os = os;
        this.deviceType = deviceType;
    }

    /**
     * Creates a SessionAttributor by extracting all supported fields from the given request.
     * @param request the HTTP servlet request
     * @return a new SessionAttributor instance with resolved fields
     */
    public static SessionAttributor forRequest(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        String deviceName = getDeviceName(request);
        String os = getOs(request);
        DeviceType deviceType = getDeviceType(request);
        return new SessionAttributor(sessionId, deviceName, os, deviceType);
    }

    /**
     * @return the resolved session ID, or null if not present
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * @return the resolved device name, or null if not present
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * @return the resolved operating system, or null if not present
     */
    public String getOs() {
        return os;
    }

    /**
     * @return the resolved device type, or {@code DeviceType.OTHER} if not present or invalid
     */
    public DeviceType getDeviceType() {
        return deviceType;
    }

    /**
     * Maps the given session ID as a request attribute.
     * @param request the HTTP servlet request
     * @param sessionId the session ID to set
     */
    public static void mapSessionId(HttpServletRequest request, String sessionId) {
        request.setAttribute(CLIENT_SESSION_ID_KEY, sessionId);
    }

    /**
     * Extracts the session ID from the request attribute.
     * @param request the HTTP servlet request
     * @return the session ID, or null if not present
     */
    private static String getSessionId(HttpServletRequest request) {
        Object attr = request.getAttribute(CLIENT_SESSION_ID_KEY);
        return attr instanceof String ? (String) attr : null;
    }

    /**
     * Extracts the device name from the request header.
     * @param request the HTTP servlet request
     * @return the device name, or null if not present
     */
    private static String getDeviceName(HttpServletRequest request) {
        return request.getHeader(DEVICE_NAME);
    }

    /**
     * Extracts the operating system from the request header.
     * @param request the HTTP servlet request
     * @return the operating system, or null if not present
     */
    private static String getOs(HttpServletRequest request) {
        return request.getHeader(OS);
    }

    /**
     * Extracts the device type from the request header.
     * @param request the HTTP servlet request
     * @return the device type, or {@code DeviceType.OTHER} if not present or invalid
     */
    private static DeviceType getDeviceType(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(DEVICE_TYPE))
                .map(typeName -> {
                    typeName = typeName.toUpperCase();
                    try {
                        return DeviceType.valueOf(typeName);
                    } catch (IllegalArgumentException e) {
                        return DeviceType.OTHER;
                    }
                }).orElse(DeviceType.OTHER);
    }
}
