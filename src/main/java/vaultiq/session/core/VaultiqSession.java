package vaultiq.session.core;

import java.time.Instant;

public final class VaultiqSession {
    private String sessionId;
    private String userId;
    private String deviceFingerPrint;
    private Instant createdAt;
    private Instant lastActiveAt;

    private VaultiqSession() {
        // avoiding external instantiation
    }

    private VaultiqSession(String sessionId, String userId, String deviceFingerPrint, Instant createdAt, Instant lastActiveAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.deviceFingerPrint = deviceFingerPrint;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
    }

    public static VaultiqSessionBuilder builder() {
        return new VaultiqSessionBuilder();
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

    public static class VaultiqSessionBuilder {
        private String sessionId;
        private String userId;
        private String deviceFingerPrint;
        private Instant createdAt;
        private Instant lastActiveAt;

        public VaultiqSessionBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public VaultiqSessionBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public VaultiqSessionBuilder deviceFingerPrint(String deviceFingerPrint) {
            this.deviceFingerPrint = deviceFingerPrint;
            return this;
        }

        public VaultiqSessionBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public VaultiqSessionBuilder lastActiveAt(Instant lastActiveAt) {
            this.lastActiveAt = lastActiveAt;
            return this;
        }

        public VaultiqSession build() {
            return new VaultiqSession(
                    sessionId,
                    userId,
                    deviceFingerPrint,
                    createdAt,
                    lastActiveAt
            );
        }
    }
}
