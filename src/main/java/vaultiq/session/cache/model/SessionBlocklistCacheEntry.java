
package vaultiq.session.cache.model;

import vaultiq.session.core.model.RevocationType;
import vaultiq.session.core.model.SessionBlocklist;

import java.time.Instant;

/**
 * Represents a cached blocklist entry for a session in the session management subsystem.
 * <p>
 * This class encapsulates all information related to the blocklisting (revocation) of a session,
 * such as session ID, user ID, revocation type, optional note, who triggered it, and timestamp.
 * It is designed for efficient in-memory caching and interoperability between
 * {@link vaultiq.session.cache.service.internal.SessionBlocklistCacheService}
 * and {@link vaultiq.session.cache.service.SessionBacklistManagerViaCache}.
 * </p>
 *
 * <p>
 * <b>Usage notes:</b><br>
 * - Typical instances are created via the static {@code create} methods for different sources (raw params or {@link SessionBlocklist}).<br>
 * - Used to quickly determine if a session is blocklisted in the cache, and to track who and why.<br>
 * - The {@link #blocklistedAt} is set to the time the entry is created (except for {@link #copy(SessionBlocklist)}).
 * </p>
 */
public class SessionBlocklistCacheEntry {
    /** The ID of the blocklisted session. */
    private String sessionId;

    /** The user ID associated with the blocklisted session. */
    private String userId;

    /** The revocation type (single, all, with exclusion, etc.). */
    private RevocationType revocationType;

    /** Optional note or reason for blocklisting. */
    private String note;

    /** The actor that triggered this blocklist entry (user or system). */
    private String triggeredBy;

    /** Timestamp (UTC) when the session was added to the blocklist. */
    private Instant blocklistedAt;

    /**
     * Constructs a new (internal) blocklist cache entry with all properties.
     *
     * @param sessionId      the session identifier
     * @param userId         the associated user identifier
     * @param revocationType the revocation strategy
     * @param note           note or reason for blocklisting
     * @param triggeredBy    the identifier of who initiated blocklisting
     * @param blocklistedAt  timestamp when blocklisted
     */
    private SessionBlocklistCacheEntry(
            String sessionId,
            String userId,
            RevocationType revocationType,
            String note,
            String triggeredBy,
            Instant blocklistedAt
    ) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.revocationType = revocationType;
        this.note = note;
        this.triggeredBy = triggeredBy;
        this.blocklistedAt = blocklistedAt;
    }

    /** Public no-arg constructor for deserialization and framework use. */
    public SessionBlocklistCacheEntry() {}

    /**
     * Factory method: create a new blocklist cache entry with the current timestamp.
     *
     * @param sessionId      session identifier
     * @param userId         associated user identifier
     * @param revocationType revocation type/strategy
     * @param note           note or reason for blocklisting
     * @param triggeredBy    who triggered the revocation
     * @return newly constructed cache entry with now() as {@link #blocklistedAt}
     */
    public static SessionBlocklistCacheEntry create(String sessionId, String userId, RevocationType revocationType, String note, String triggeredBy) {
        return new SessionBlocklistCacheEntry(
                sessionId,
                userId,
                revocationType,
                note,
                triggeredBy,
                Instant.now() // use current moment for new blocklisting
        );
    }

    /**
     * Factory method: creates a new blocklist cache entry from a {@link SessionBlocklist},
     * using current time as {@link #blocklistedAt}.
     *
     * @param sessionBlocklist the session blocklist model instance
     * @return new cache entry
     */
    public static SessionBlocklistCacheEntry create(SessionBlocklist sessionBlocklist) {
        return new SessionBlocklistCacheEntry(
                sessionBlocklist.getSessionId(),
                sessionBlocklist.getUserId(),
                sessionBlocklist.getRevocationType(),
                sessionBlocklist.getNote(),
                sessionBlocklist.getTriggeredBy(),
                Instant.now()
        );
    }

    /**
     * Factory method: creates a copy of the given SessionBlocklist, including the original blocklistedAt timestamp.
     *
     * @param sessionBlocklist the input model
     * @return new cache entry with the same properties as the model (preserves time)
     */
    public static SessionBlocklistCacheEntry copy(SessionBlocklist sessionBlocklist) {
        return new SessionBlocklistCacheEntry(
                sessionBlocklist.getSessionId(),
                sessionBlocklist.getUserId(),
                sessionBlocklist.getRevocationType(),
                sessionBlocklist.getNote(),
                sessionBlocklist.getTriggeredBy(),
                sessionBlocklist.getBlocklistedAt()
        );
    }

    /** @return the blocklisted session ID */
    public String getSessionId() {
        return sessionId;
    }

    /** @return the user ID associated with the session */
    public String getUserId() {
        return userId;
    }

    /** @return the type of revocation used for this entry */
    public RevocationType getRevocationType() {
        return revocationType;
    }

    /** @return optional note or reason for blocklisting */
    public String getNote() {
        return note;
    }

    /** @return the actor (user or system) who triggered the blocklisting */
    public String getTriggeredBy() {
        return triggeredBy;
    }

    /** @return the timestamp when the entry was added to the blocklist */
    public Instant getBlocklistedAt() {
        return blocklistedAt;
    }

    /** Sets the session ID. */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /** Sets the user ID. */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /** Sets the revocation type for this entry. */
    public void setRevocationType(RevocationType revocationType) {
        this.revocationType = revocationType;
    }

    /** Sets the note or reason for this entry. */
    public void setNote(String note) {
        this.note = note;
    }

    /** Sets the actor who triggered the blocklisting. */
    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    /** Sets the timestamp for when the session was blocklisted. */
    public void setBlocklistedAt(Instant blocklistedAt) {
        this.blocklistedAt = blocklistedAt;
    }
}
