package vaultiq.session.jpa.revoke.model;

import jakarta.persistence.*;
import vaultiq.session.core.model.RevocationType;
import java.time.Instant;

/**
 * Represents a revoked session entity persisted in the database.
 * <p>
 * This JPA entity maps to the {@code revoked_session_pool} table and stores
 * details about sessions that have been marked as revoked (invalidated).
 * <p>
 * It captures information such as the session's unique identifier, associated user ID,
 * the revocation type, any notes explaining the revocation, the actor who triggered
 * the revocation, and the timestamp when the revocation occurred.
 * <p>
 * This entity is used internally by the persistence layer to manage revoked
 * session records and is key to enforcing session invalidation in the system.
 */
@Entity
@Table(
        name = "revoked_session_pool",
        indexes = {
                @Index(name = "idx_revoked_at", columnList = "revoked_at")
        }
)
public class RevokedSessionEntity {

    /**
     * The unique identifier of the revoked session.
     * This is the primary key for the entity and is immutable.
     */
    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private String sessionId;

    /**
     * The user ID associated with the revoked session.
     * This field is immutable after creation.
     */
    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    /**
     * The revocation type applied to this session.
     * Stored as a string representation of the enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "revocation_type", nullable = false)
    private RevocationType revocationType;

    /**
     * Optional note providing context or reason for the revocation.
     */
    @Column(name = "note")
    private String note;

    /**
     * Identifier of the user or system that triggered the revocation.
     * Cannot be null.
     */
    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    /**
     * Timestamp (UTC) when the session was revoked.
     * This is immutable after creation.
     */
    @Column(name = "revoked_at", nullable = false, updatable = false)
    private Instant revokedAt;

    // Getters and Setters

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public RevocationType getRevocationType() {
        return revocationType;
    }

    public void setRevocationType(RevocationType revocationType) {
        this.revocationType = revocationType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
