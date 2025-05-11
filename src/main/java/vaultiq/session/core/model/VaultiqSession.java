package vaultiq.session.core.model;

import java.time.Instant;

public final class VaultiqSession {
    private String sessionId;
    private String userId;
    private String deviceFingerPrint;
    private Instant createdAt;
    private boolean isBlocked;
    private Instant blockedAt;

    private VaultiqSession() {
        // avoiding external instantiation
    }

    private VaultiqSession(
            String sessionId,
            String userId,
            String deviceFingerPrint,
            Instant createdAt,
            boolean isBlocked,
            Instant blockedAt
    ) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.deviceFingerPrint = deviceFingerPrint;
        this.createdAt = createdAt;
        this.isBlocked = isBlocked;
        this.blockedAt = blockedAt;
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

    public boolean isBlocked() {
        return isBlocked;
    }

    public Instant getBlockedAt() {
        return blockedAt;
    }

    public static class VaultiqSessionBuilder {
        private String sessionId;
        private String userId;
        private String deviceFingerPrint;
        private Instant createdAt;
        private boolean isBlocked;
        private Instant blockedAt;

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

        public VaultiqSessionBuilder isBlocked(boolean isBlocked) {
            this.isBlocked = isBlocked;
            return this;
        }

        public VaultiqSessionBuilder blockedAt(Instant blockedAt) {
            this.blockedAt = blockedAt;
            return this;
        }

        public VaultiqSession build() {
            return new VaultiqSession(
                    sessionId,
                    userId,
                    deviceFingerPrint,
                    createdAt,
                    isBlocked,
                    blockedAt
            );
        }
    }
}
