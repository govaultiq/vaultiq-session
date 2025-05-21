package vaultiq.session.cache.model;

import vaultiq.session.cache.service.SessionRevocationManagerViaCache;
import vaultiq.session.cache.service.internal.SessionRevocationCacheService;
import vaultiq.session.core.model.RevocationType;
import vaultiq.session.core.model.RevokedSession;
import java.time.Instant;

/**
 * Represents a cached revocation entry for a session in the session management subsystem.
 * <p>
 * Encapsulates information about a session marked as revoked, including session ID,
 * user ID, revocation type, optional note, who triggered it, and timestamp.
 * Designed for efficient in-memory caching and interoperability between
 * {@link SessionRevocationCacheService}
 * and {@link SessionRevocationManagerViaCache}.
 * </p>
 *
 * <p><b>Usage notes:</b>
 * <ul>
 *   <li>Typically created via {@link #create(String, String, RevocationType, String, String)}</li>
 *   <li>Or from a {@link RevokedSession} via {@link #fromModel(RevokedSession)}</li>
 *   <li>Or as an exact copy preserving original timestamp via {@link #copy(RevokedSession)}</li>
 * </ul>
 * </p>
 */
public class RevokedSessionCacheEntry {

    /** The ID of the revoked session. */
    private String sessionId;

    /** The user ID associated with the revoked session. */
    private String userId;

    /** The revocation type (single, all, with exclusion, etc.). */
    private RevocationType revocationType;

    /** Optional note or reason for revocation. */
    private String note;

    /** The actor that triggered this revocation entry (user or system). */
    private String triggeredBy;

    /** Timestamp (UTC) when the session was revoked. */
    private Instant revokedAt;

    /**
     * Internal constructor with all properties.
     */
    private RevokedSessionCacheEntry(String sessionId,
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

    /** Public no-arg constructor for framework use. */
    public RevokedSessionCacheEntry() {}

    /**
     * Creates a new cache entry with the current timestamp.
     */
    public static RevokedSessionCacheEntry create(String sessionId,
                                                  String userId,
                                                  RevocationType revocationType,
                                                  String note,
                                                  String triggeredBy) {
        return new RevokedSessionCacheEntry(
                sessionId, userId, revocationType, note, triggeredBy, Instant.now()
        );
    }

    /**
     * Creates a new cache entry from a model, using current time.
     */
    public static RevokedSessionCacheEntry fromModel(RevokedSession model) {
        return new RevokedSessionCacheEntry(
                model.getSessionId(),
                model.getUserId(),
                model.getRevocationType(),
                model.getNote(),
                model.getTriggeredBy(),
                Instant.now()
        );
    }

    /**
     * Creates a copy of the given model, preserving its original timestamp.
     */
    public static RevokedSessionCacheEntry copy(RevokedSession model) {
        return new RevokedSessionCacheEntry(
                model.getSessionId(),
                model.getUserId(),
                model.getRevocationType(),
                model.getNote(),
                model.getTriggeredBy(),
                model.getRevokedAt()
        );
    }

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