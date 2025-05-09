package vaultiq.session.core.model;

import java.time.Instant;

public final class UserActivityRecord {
    private String userId;
    private Instant lastActiveAt;
    private String sessionUsed;

    private UserActivityRecord() {
        // Avoiding external instantiation
    }

    private UserActivityRecord(String userId, Instant lastActiveAt, String sessionUsed) {
        this.userId = userId;
        this.lastActiveAt = lastActiveAt;
        this.sessionUsed = sessionUsed;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public String getSessionUsed() {
        return sessionUsed;
    }

    public static UserActivityRecordBuilder builder() {
        return new UserActivityRecordBuilder();
    }

    /**
     * Initialize a new UserActivityRecord with the given userId, lastActiveAt, and sessionId.
     * <p>
     *
     * @param userId    the user identifier
     * @param sessionId the session identifier
     * @return the initialized {@link UserActivityRecord}
     */
    public static UserActivityRecord init(String userId, String sessionId) {
        return new UserActivityRecord(userId, Instant.now(), sessionId);
    }

    public static class UserActivityRecordBuilder {
        private String userId;
        private Instant lastActiveAt;
        private String sessionUsed;

        public UserActivityRecordBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public UserActivityRecordBuilder lastActiveAt(Instant lastActiveAt) {
            this.lastActiveAt = lastActiveAt;
            return this;
        }

        public UserActivityRecordBuilder sessionUsed(String sessionUsed) {
            this.sessionUsed = sessionUsed;
            return this;
        }

        public UserActivityRecord build() {
            return new UserActivityRecord(userId, lastActiveAt, sessionUsed);
        }
    }
}
