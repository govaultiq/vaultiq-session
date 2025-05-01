package vaultiq.session.cache.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public final class VaultiqSessionCacheEntry implements Serializable {
    private String sessionId;
    private String userId;
    private String deviceFingerPrint;
    private Instant createdAt;
    private Instant lastActiveAt;

    private VaultiqSessionCacheEntry() {
        // Avoiding Direct Instantiation
    }

    public static VaultiqSessionCacheEntry create(String userId, String deviceFingerPrint){
        VaultiqSessionCacheEntry vaultiqSession = new VaultiqSessionCacheEntry();
        vaultiqSession.sessionId = UUID.randomUUID().toString();
        vaultiqSession.userId = userId;
        vaultiqSession.deviceFingerPrint = deviceFingerPrint;
        vaultiqSession.createdAt = Instant.now();
        vaultiqSession.lastActiveAt = Instant.now();
        return vaultiqSession;
    }

    public void currentlyActive() {
        this.lastActiveAt = Instant.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getDeviceFingerPrint() {
        return deviceFingerPrint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VaultiqSessionCacheEntry that = (VaultiqSessionCacheEntry) o;

        return sessionId.equals(that.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }

    @Override
    public String toString() {
        return "VaultiqSession{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", createdAt=" + createdAt +
                ", lastActiveAt=" + lastActiveAt +
                '}';
    }

}
