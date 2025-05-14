package vaultiq.session.core.model;

import java.time.Instant;

/**
 * Represents a single blocklisted (revoked or invalidated) session.
 * <p>
 * This is the primary Data Transfer Object (DTO) used by the {@code vaultiq-session}
 * library to expose information about a session that has been marked as invalid
 * to consuming applications. It provides a consistent view of blocklisted session
 * data regardless of the underlying persistence mechanism.
 * </p>
 * <p>
 * {@code SessionBlocklist} objects are typically returned by methods in
 * {@link vaultiq.session.core.SessionBacklistManager} and are intended for use
 * in application logic, APIs, and UI layers that need to display or process
 * blocklisted session information.
 * </p>
 * <p>
 * Each instance encapsulates key details about the blocklisting event: which session
 * and user were affected, the type of revocation, who triggered the action, and
 * when it occurred.
 * </p>
 *
 * @see vaultiq.session.core.SessionBacklistManager
 * @see RevocationType
 */
public class SessionBlocklist {
    /**
     * The unique identifier of the blocklisted session.
     */
    private String sessionId;

    /**
     * The user ID to whom the blocklisted session belongs.
     */
    private String userId;

    /**
     * The type or strategy of revocation applied to this session.
     */
    private RevocationType revocationType;

    /**
     * An optional human-readable note or reason provided for the blocklisting.
     */
    private String note;

    /**
     * The identifier of the user or system principal who performed the blocklisting action.
     */
    private String triggeredBy;

    /**
     * The timestamp (UTC) when the session was blocklisted.
     */
    private Instant blocklistedAt;

    /**
     * Constructs a fully defined SessionBlocklist object.
     * <p>
     * Users are recommended to use the {@link #builder()} method for a more
     * readable and potentially more stable way to create instances.
     * </p>
     *
     * @param sessionId      The unique identifier of the blocklisted session.
     * @param userId         The user ID to whom the session belongs.
     * @param revocationType The type of revocation applied.
     * @param note           An optional note.
     * @param triggeredBy    The actor who triggered the blocklisting.
     * @param blocklistedAt  The timestamp of the blocklisting.
     */
    private SessionBlocklist(String sessionId, String userId, RevocationType revocationType, String note, String triggeredBy, Instant blocklistedAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.revocationType = revocationType;
        this.note = note;
        this.triggeredBy = triggeredBy;
        this.blocklistedAt = blocklistedAt;
    }

    /**
     * Protected constructor for use by serialization frameworks (e.g., JPA, Jackson).
     */
    private SessionBlocklist() {
        // Protected for framework use.
    }

    /**
     * Returns the unique identifier of the blocklisted session.
     *
     * @return the session ID.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the user ID associated with the blocklisted session.
     *
     * @return the user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the type of revocation applied to this session.
     *
     * @return the {@link RevocationType}.
     */
    public RevocationType getRevocationType() {
        return revocationType;
    }

    /**
     * Returns the optional note providing context for the blocklisting.
     *
     * @return the note string, or {@code null}.
     */
    public String getNote() {
        return note;
    }

    /**
     * Returns the identifier of the actor who triggered the blocklisting.
     *
     * @return the triggered by identifier.
     */
    public String getTriggeredBy() {
        return triggeredBy;
    }

    /**
     * Returns the timestamp (UTC) when the session was blocklisted.
     *
     * @return the blocklisted timestamp.
     */
    public Instant getBlocklistedAt() {
        return blocklistedAt;
    }

    /**
     * Returns a builder for constructing {@link SessionBlocklist} instances.
     * <p>
     * Using the builder pattern is the recommended way to create
     * {@code SessionBlocklist} objects.
     * </p>
     *
     * @return a new {@link SessionBlocklistBuilder}.
     */
    public static SessionBlocklistBuilder builder() {
        return new SessionBlocklistBuilder();
    }

    /**
     * Builder class for creating {@link SessionBlocklist} instances.
     * <p>
     * Provides a fluent API for setting the properties of a {@code SessionBlocklist}
     * before building the final immutable object.
     * </p>
     */
    public static class SessionBlocklistBuilder {
        private String sessionId;
        private String userId;
        private RevocationType revocationType;
        private String note;
        private String triggeredBy;
        private Instant blocklistedAt;

        /**
         * Sets the session ID for the blocklist entry.
         *
         * @param sessionId the blocklisted session ID.
         * @return the builder instance.
         */
        public SessionBlocklistBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * Sets the user ID for the blocklist entry.
         *
         * @param userId the user the session belongs to.
         * @return the builder instance.
         */
        public SessionBlocklistBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the revocation type for the blocklist entry.
         *
         * @param revocationType the {@link RevocationType}.
         * @return the builder instance.
         */
        public SessionBlocklistBuilder revocationType(RevocationType revocationType) {
            this.revocationType = revocationType;
            return this;
        }

        /**
         * Sets an optional note for the blocklist entry.
         *
         * @param note a human-readable note.
         * @return the builder instance.
         */
        public SessionBlocklistBuilder note(String note) {
            this.note = note;
            return this;
        }

        /**
         * Sets the identifier of the actor who triggered the blocklisting.
         *
         * @param triggeredBy the triggered by identifier.
         * @return the builder instance.
         */
        public SessionBlocklistBuilder triggeredBy(String triggeredBy) {
            this.triggeredBy = triggeredBy;
            return this;
        }

        /**
         * Sets the timestamp (UTC) when the session was blocklisted.
         *
         * @param blocklistedAt the blocklisted timestamp.
         * @return the builder instance.
         */
        public SessionBlocklistBuilder blocklistedAt(Instant blocklistedAt) {
            this.blocklistedAt = blocklistedAt;
            return this;
        }

        /**
         * Builds the final {@link SessionBlocklist} instance with the configured properties.
         *
         * @return the constructed {@link SessionBlocklist}.
         */
        public SessionBlocklist build() {
            return new SessionBlocklist(sessionId, userId, revocationType, note, triggeredBy, blocklistedAt);
        }
    }
}
