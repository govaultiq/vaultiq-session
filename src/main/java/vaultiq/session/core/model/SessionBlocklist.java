
package vaultiq.session.core.model;

import java.time.Instant;

/**
 * Represents a single blocklisted (revoked or invalidated) session in the application/session security API.
 *
 * <p>
 * This is the core DTO model exposed by all {@code SessionBacklistManager} implementations and higher-level APIs
 * for listing, auditing, and querying blocklisted session data. It is not tied to a particular persistence strategy.
 * Use this to safely pass blocklist details between service, controller, or API layers.
 * </p>
 *
 * <p>
 * Blocklisting a session means it is no longer valid for authentication or API access until
 * the blocklist entry is removed (if supported). Each entry encapsulates WHY, WHO, and WHEN
 * a session was blocklisted, as well as which session and user it relates to.
 * </p>
 *
 * <p>
 * <b>Usage examples:</b>
 * <ul>
 *   <li>Returned from {@code SessionBacklistManager#getBlocklistedSessions(userId)} for APIs and UI.</li>
 *   <li>Used as a read model for session-security microservices and administration tools.</li>
 *   <li>Migrates easily between cache, DB, or distributed sources (see {@code SessionBlocklistCacheEntry}, {@code SessionBlocklistEntity}).</li>
 * </ul>
 * </p>
 *
 * @see vaultiq.session.core.SessionBacklistManager
 * @see vaultiq.session.jpa.model.SessionBlocklistEntity
 * @see vaultiq.session.cache.model.SessionBlocklistCacheEntry
 */
public class SessionBlocklist {
    /** The blocklisted session's unique identifier. */
    private String sessionId;

    /** The user ID to whom the session belongs. */
    private String userId;

    /** The revocation strategy applied to this blocklisting (single-session, all, with exclusion, etc). */
    private RevocationType revocationType;

    /** Human-readable note or reason for blocklisting, if provided. */
    private String note;

    /** The user or system principal who performed the blocklisting. */
    private String triggeredBy;

    /** The time (UTC) when the session was blocklisted. */
    private Instant blocklistedAt;

    /**
     * Constructs a fully defined SessionBlocklist object.
     * Recommended to use {@link #builder()} instead for forward compatibility.
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
     * Protected constructor for serialization/deserialization and frameworks.
     */
    private SessionBlocklist() {
        // Avoiding External Instantiation
    }

    /** @return the unique blocklisted session identifier */
    public String getSessionId() {
        return sessionId;
    }

    /** @return the user identifier for the blocklisted session */
    public String getUserId() {
        return userId;
    }

    /** @return the session revocation type/strategy */
    public RevocationType getRevocationType() {
        return revocationType;
    }

    /** @return any additional note or reason for blocklisting */
    public String getNote() {
        return note;
    }

    /** @return the actor (user/system) who triggered the blocklisting */
    public String getTriggeredBy() {
        return triggeredBy;
    }

    /** @return the timestamp the session was blocklisted (UTC) */
    public Instant getBlocklistedAt() {
        return blocklistedAt;
    }

    /**
     * Returns a builder for easy, type-safe construction of SessionBlocklist DTOs.
     * Use builder pattern for forward compatibility.
     *
     * @return a new {@link SessionBlocklistBuilder}
     */
    public static SessionBlocklistBuilder builder() {
        return new SessionBlocklistBuilder();
    }

    /**
     * Builder for {@link SessionBlocklist}.
     * Allows convenient and readable step-by-step property assignment.
     */
    public static class SessionBlocklistBuilder {
        private String sessionId;
        private String userId;
        private RevocationType revocationType;
        private String note;
        private String triggeredBy;
        private Instant blocklistedAt;

        /** @param sessionId the blocklisted session ID */
        public SessionBlocklistBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /** @param userId the user the session belongs to */
        public SessionBlocklistBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /** @param revocationType the revocation reason/type */
        public SessionBlocklistBuilder revocationType(RevocationType revocationType) {
            this.revocationType = revocationType;
            return this;
        }

        /** @param note a human-readable note about the blocklisting */
        public SessionBlocklistBuilder note(String note) {
            this.note = note;
            return this;
        }

        /** @param triggeredBy the actor who triggered the blocklist event */
        public SessionBlocklistBuilder triggeredBy(String triggeredBy) {
            this.triggeredBy = triggeredBy;
            return this;
        }

        /** @param blocklistedAt the UTC time the blocklist entry was created */
        public SessionBlocklistBuilder blocklistedAt(Instant blocklistedAt) {
            this.blocklistedAt = blocklistedAt;
            return this;
        }

        /**
         * Builds an immutable SessionBlocklist with the assigned properties.
         *
         * @return a completed {@link SessionBlocklist} DTO
         */
        public SessionBlocklist build() {
            return new SessionBlocklist(sessionId, userId, revocationType, note, triggeredBy, blocklistedAt);
        }
    }
}
