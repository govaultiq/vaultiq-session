package vaultiq.session.jpa.model;

import jakarta.persistence.*;
import vaultiq.session.core.model.RevocationType;

import java.time.Instant;

@Entity
@Table(name = "blocklist")
public class SessionBlocklistEntity {

    @Id
    @Column(name = "session_id", nullable = false, updatable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "revocation_type", nullable = false)
    private RevocationType revocationType;

    @Column(name = "note")
    private String note;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @Column(name = "blocklisted_at", nullable = false, updatable = false)
    private Instant blocklistedAt;

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

    public Instant getBlocklistedAt() {
        return blocklistedAt;
    }

    public void setBlocklistedAt(Instant blocklistedAt) {
        this.blocklistedAt = blocklistedAt;
    }
}
