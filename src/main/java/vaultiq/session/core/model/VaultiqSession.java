package vaultiq.session.core.model;

import java.time.Instant;
import vaultiq.session.core.VaultiqSessionManager;

/**
 * Represents a single Vaultiq session.
 * <p>
 * This is a core Data Transfer Object (DTO) used by the {@code vaultiq-session}
 * library to represent an active or blocklisted user session. It encapsulates
 * key information about the session, such as its unique identifier, the associated
 * user, device details, creation time, and its current blocklisted status.
 * </p>
 * <p>
 * {@code VaultiqSession} objects are typically returned by methods in
 * {@link VaultiqSessionManager} and are intended for use in application logic,
 * APIs, and UI layers that need to display or process session information.
 * </p>
 * <p>
 * Instances of this class are immutable and should be created using the
 * provided {@link #builder()}.
 * </p>
 *
 * @see VaultiqSessionManager
 * @see RevokedSession
 */
public final class VaultiqSession {
    /**
     * The unique identifier for this session.
     */
    private String sessionId;
    /**
     * The unique identifier of the user associated with this session.
     */
    private String userId;
    /**
     * A fingerprint or identifier representing the device from which the session originated.
     */
    private String deviceFingerPrint;
    /**
     * The timestamp (UTC) when this session was created.
     */
    private Instant createdAt;
    /**
     * Indicates whether this session is currently blocklisted.
     */
    private boolean isBlocked;
    /**
     * The timestamp (UTC) when this session was blocklisted, if applicable.
     * This will be {@code null} if the session is not blocked.
     */
    private Instant blockedAt;

    /**
     * Private constructor to prevent direct instantiation.
     * Instances should be created using the {@link #builder()}.
     */
    private VaultiqSession() {
        // avoiding external instantiation
    }

    /**
     * Constructs a new VaultiqSession with all properties.
     * <p>
     * This constructor is intended for internal use by the {@link VaultiqSessionBuilder}.
     * Consuming applications should use the builder pattern via {@link #builder()}.
     * </p>
     *
     * @param sessionId         The unique identifier for this session.
     * @param userId            The unique identifier of the user.
     * @param deviceFingerPrint The device fingerprint.
     * @param createdAt         The timestamp when the session was created.
     * @param isBlocked         Whether the session is blocked.
     * @param blockedAt         The timestamp when the session was blocked (can be null).
     */
    private VaultiqSession(
            String sessionId,
            String userId,
            String deviceFingerPrint,
            Instant createdAt,
            boolean isBlocked,
            Instant blockedAt
    ) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.deviceFingerPrint = deviceFingerPrint;
        this.createdAt = createdAt;
        this.isBlocked = isBlocked;
        this.blockedAt = blockedAt;
    }

    /**
     * Returns a new builder for creating {@link VaultiqSession} instances.
     * <p>
     * Using the builder pattern is the recommended way to construct
     * {@code VaultiqSession} objects.
     * </p>
     *
     * @return a new {@link VaultiqSessionBuilder}.
     */
    public static VaultiqSessionBuilder builder() {
        return new VaultiqSessionBuilder();
    }

    /**
     * Returns the unique identifier for this session.
     *
     * @return the session ID.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the unique identifier of the user associated with this session.
     *
     * @return the user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the fingerprint or identifier representing the device.
     *
     * @return the device fingerprint.
     */
    public String getDeviceFingerPrint() {
        return deviceFingerPrint;
    }

    /**
     * Returns the timestamp (UTC) when this session was created.
     *
     * @return the creation timestamp.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Indicates whether this session is currently blocklisted.
     *
     * @return {@code true} if the session is blocked, {@code false} otherwise.
     */
    public boolean isBlocked() {
        return isBlocked;
    }

    /**
     * Returns the timestamp (UTC) when this session was blocklisted.
     *
     * @return the blocklisted timestamp, or {@code null} if not blocked.
     */
    public Instant getBlockedAt() {
        return blockedAt;
    }

    /**
     * Builder class for creating immutable {@link VaultiqSession} instances.
     * <p>
     * Provides a fluent API for setting the properties of a {@code VaultiqSession}
     * before building the final object.
     * </p>
     */
    public static class VaultiqSessionBuilder {
        private String sessionId;
        private String userId;
        private String deviceFingerPrint;
        private Instant createdAt;
        private boolean isBlocked;
        private Instant blockedAt;

        /**
         * Sets the session ID.
         * @param sessionId the session ID.
         * @return the builder instance.
         */
        public VaultiqSessionBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * Sets the user ID.
         * @param userId the user ID.
         * @return the builder instance.
         */
        public VaultiqSessionBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the device fingerprint.
         * @param deviceFingerPrint the device fingerprint.
         * @return the builder instance.
         */
        public VaultiqSessionBuilder deviceFingerPrint(String deviceFingerPrint) {
            this.deviceFingerPrint = deviceFingerPrint;
            return this;
        }

        /**
         * Sets the creation timestamp.
         * @param createdAt the creation timestamp.
         * @return the builder instance.
         */
        public VaultiqSessionBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the blocklisted status.
         * @param isBlocked {@code true} if blocked, {@code false} otherwise.
         * @return the builder instance.
         */
        public VaultiqSessionBuilder isBlocked(boolean isBlocked) {
            this.isBlocked = isBlocked;
            return this;
        }

        /**
         * Sets the blocklisted timestamp.
         * @param blockedAt the blocklisted timestamp (can be null).
         * @return the builder instance.
         */
        public VaultiqSessionBuilder blockedAt(Instant blockedAt) {
            this.blockedAt = blockedAt;
            return this;
        }

        /**
         * Builds the final immutable {@link VaultiqSession} instance with the configured properties.
         *
         * @return the constructed {@link VaultiqSession}.
         */
        public VaultiqSession build() {
            return new VaultiqSession(
                    sessionId,
                    userId,
                    deviceFingerPrint,
                    createdAt,
                    isBlocked,
                    blockedAt
            );
        }
    }
}
