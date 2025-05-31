package vaultiq.session.cache.model;

import vaultiq.session.domain.model.UserActivityRecord;

import java.time.Instant;

public final class UserActivityRecordCacheEntry {
    private String userId;
    private Instant lastActiveAt;
    private String sessionUsed;

    private UserActivityRecordCacheEntry() {
        // Avoiding external instantiation
    }

    private UserActivityRecordCacheEntry(String userId, Instant lastActiveAt, String sessionUsed) {
        this.userId = userId;
        this.lastActiveAt = lastActiveAt;
        this.sessionUsed = sessionUsed;
    }

    /**
     * Useful to copy the {@link UserActivityRecord} to UserActivityRecordCacheEntry to cache.
     * <p>
     *
     * @param source the source {@link UserActivityRecord}
     * @return the {@link UserActivityRecordCacheEntry}
     */
    public static UserActivityRecordCacheEntry copy(UserActivityRecord source) {
        return new UserActivityRecordCacheEntry(source.getUserId(), source.getLastActiveAt(), source.getSessionUsed());
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
}
