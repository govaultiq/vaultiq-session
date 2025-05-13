package vaultiq.session.cache.model;

import vaultiq.session.core.model.RevocationType;
import vaultiq.session.core.model.SessionBlocklist;

import java.time.Instant;

public class SessionBlocklistCacheEntry {
    private String sessionId;
    private String userId;
    private RevocationType revocationType;
    private String note;
    private String triggeredBy;
    private Instant blocklistedAt;

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

    public SessionBlocklistCacheEntry() {
    }

    public static SessionBlocklistCacheEntry create(String sessionId, String userId, RevocationType revocationType, String note, String triggeredBy) {
        return new SessionBlocklistCacheEntry(
                sessionId,
                userId,
                revocationType,
                note,
                triggeredBy,
                Instant.now()
        );
    }

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

    public Instant getBlocklistedAt() {
        return blocklistedAt;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setRevocationType(RevocationType revocationType) {
        this.revocationType = revocationType;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public void setBlocklistedAt(Instant blocklistedAt) {
        this.blocklistedAt = blocklistedAt;
    }
}
