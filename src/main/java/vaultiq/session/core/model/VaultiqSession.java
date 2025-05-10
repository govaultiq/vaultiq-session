package vaultiq.session.core.model;

import java.time.Instant;

public final class VaultiqSession {
    private String sessionId;
    private String userId;
    private String deviceFingerPrint;
    private Instant createdAt;

    private VaultiqSession() {
        // avoiding external instantiation
    }

    private VaultiqSession(String sessionId, String userId, String deviceFingerPrint, Instant createdAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.deviceFingerPrint = deviceFingerPrint;
        this.createdAt = createdAt;
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


    public static class VaultiqSessionBuilder {
        private String sessionId;
        private String userId;
        private String deviceFingerPrint;
        private Instant createdAt;

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

        public VaultiqSession build() {
            return new VaultiqSession(
                    sessionId,
                    userId,
                    deviceFingerPrint,
                    createdAt
            );
        }
    }
}
