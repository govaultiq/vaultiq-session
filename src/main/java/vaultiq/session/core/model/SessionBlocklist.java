package vaultiq.session.core.model;

import java.time.Instant;

public class SessionBlocklist {
    private String sessionId;
    private String userId;
    private RevocationType revocationType;
    private String note;
    private String triggeredBy;
    private Instant blocklistedAt;

    private SessionBlocklist(String sessionId, String userId, RevocationType revocationType, String note, String triggeredBy, Instant blocklistedAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.revocationType = revocationType;
        this.note = note;
        this.triggeredBy = triggeredBy;
        this.blocklistedAt = blocklistedAt;
    }

    private SessionBlocklist() {
        // Avoiding External Instantiation
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

    public SessionBlocklistBuilder builder() {
        return new SessionBlocklistBuilder();
    }

    public static class SessionBlocklistBuilder {
        private String sessionId;
        private String userId;
        private RevocationType revocationType;
        private String note;
        private String triggeredBy;
        private Instant blocklistedAt;

        public SessionBlocklistBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public SessionBlocklistBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public SessionBlocklistBuilder revocationType(RevocationType revocationType) {
            this.revocationType = revocationType;
            return this;
        }

        public SessionBlocklistBuilder note(String note) {
            this.note = note;
            return this;
        }

        public SessionBlocklistBuilder triggeredBy(String triggeredBy) {
            this.triggeredBy = triggeredBy;
            return this;
        }

        public SessionBlocklistBuilder blocklistedAt(Instant blocklistedAt) {
            this.blocklistedAt = blocklistedAt;
            return this;
        }

        public SessionBlocklist build() {
            return new SessionBlocklist(sessionId, userId, revocationType, note, triggeredBy, blocklistedAt);
        }
    }
}
