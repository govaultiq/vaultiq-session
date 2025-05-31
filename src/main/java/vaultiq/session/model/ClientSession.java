package vaultiq.session.model;

import java.time.Instant;

import vaultiq.session.core.service.SessionManager;

/**
 * Represents a single Vaultiq session.
 * <p>
 * This is a core Data Transfer Object (DTO) used by the {@code vaultiq-session}
 * library to represent an active or blocklisted user session. It encapsulates
 * key information about the session, such as its unique identifier, the associated
 * user, device details, creation time, and its current blocklisted status.
 * </p>
 * <p>
 * {@code ClientSession} objects are typically returned by methods in
 * {@link SessionManager} and are intended for use in application logic,
 * APIs, and UI layers that need to display or process session information.
 * </p>
 * <p>
 * Instances of this class are immutable and should be created using the
 * provided {@link #builder()}.
 * </p>
 *
 * @see SessionManager
 * @see RevokedSession
 */
public final class ClientSession {
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
    private boolean isRevoked;
    /**
     * The timestamp (UTC) when this session was blocklisted, if applicable.
     * This will be {@code null} if the session is not blocked.
     */
    private Instant revokedAt;

    /**
     * Private constructor to prevent direct instantiation.
     * Instances should be created using the {@link #builder()}.
     */
    private ClientSession() {
        // avoiding external instantiation
    }

    /**
     * Constructs a new ClientSession with all properties.
     * <p>
     * This constructor is intended for internal use by the {@link VaultiqSessionBuilder}.
     * Consuming applications should use the builder pattern via {@link #builder()}.
     * </p>
     *
     * @param sessionId         The unique identifier for this session.
     * @param userId            The unique identifier of the user.
     * @param deviceFingerPrint The device fingerprint.
     * @param createdAt         The timestamp when the session was created.
     * @param isRevoked         Whether the session is blocked.
     * @param revokedAt         The timestamp when the session was blocked (can be null).
     */
    private ClientSession(
            String sessionId,
            String userId,
            String deviceFingerPrint,
            Instant createdAt,
            boolean isRevoked,
            Instant revokedAt
    ) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.deviceFingerPrint = deviceFingerPrint;
        this.createdAt = createdAt;
        this.isRevoked = isRevoked;
        this.revokedAt = revokedAt;
    }

    /**
     * Returns a new builder for creating {@link ClientSession} instances.
     * <p>
     * Using the builder pattern is the recommended way to construct
     * {@code ClientSession} objects.
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
    public boolean isRevoked() {
        return isRevoked;
    }

    /**
     * Returns the timestamp (UTC) when this session was blocklisted.
     *
     * @return the blocklisted timestamp, or {@code null} if not blocked.
     */
    public Instant getRevokedAt() {
        return revokedAt;
    }

    /**
     * Builder class for creating immutable {@link ClientSession} instances.
     * <p>
     * Provides a fluent API for setting the properties of a {@code ClientSession}
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
         * Builds the final immutable {@link ClientSession} instance with the configured properties.
         *
         * @return the constructed {@link ClientSession}.
         */
        public ClientSession build() {
            return new ClientSession(
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
