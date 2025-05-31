package vaultiq.session.model;

import vaultiq.session.core.service.SessionRevocationManager;
import java.time.Instant;

/**
 * Represents a single revoked session.
 * <p>
 * This DTO exposes information about a session marked as revoked
 * for consuming applications. It provides a consistent view regardless
 * of the underlying persistence mechanism.
 * </p>
 * <p>
 * Instances are typically returned by methods in {@link SessionRevocationManager}
 * and used in application logic, APIs, or UI layers.
 * </p>
 * <p>
 * Each instance contains details about the revocation event: session ID,
 * user ID, revocation type, actor, note, and timestamp.
 * </p>
 *
 * @see SessionRevocationManager
 * @see RevocationType
 */
public class RevokedSession {

    private String sessionId;
    private String userId;
    private RevocationType revocationType;
    private String note;
    private String triggeredBy;
    private Instant revokedAt;

    private RevokedSession(String sessionId,
                           String userId,
                           RevocationType revocationType,
                           String note,
                           String triggeredBy,
                           Instant revokedAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.revocationType = revocationType;
        this.note = note;
        this.triggeredBy = triggeredBy;
        this.revokedAt = revokedAt;
    }

    private RevokedSession() {
        // for frameworks
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public RevocationType getRevocationType() {
        return revocationType;
    }

    public String getNote() {
        return note;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    /**
     * Returns a builder for creating {@link RevokedSession} instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RevokedSession}.
     */
    public static class Builder {
        private String sessionId;
        private String userId;
        private RevocationType revocationType;
        private String note;
        private String triggeredBy;
        private Instant revokedAt;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder revocationType(RevocationType revocationType) {
            this.revocationType = revocationType;
            return this;
        }

        public Builder note(String note) {
            this.note = note;
            return this;
        }

        public Builder triggeredBy(String triggeredBy) {
            this.triggeredBy = triggeredBy;
            return this;
        }

        public Builder revokedAt(Instant revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public RevokedSession build() {
            return new RevokedSession(
                    sessionId, userId, revocationType,
                    note, triggeredBy, revokedAt
            );
        }
    }
}
