package vaultiq.session.jpa.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "vaultiq_session_pool")
public final class JpaVaultiqSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Column(name = "device_finger_print", nullable = false, updatable = false)
    private String deviceFingerPrint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static JpaVaultiqSession create(String userId, String deviceFingerPrint) {
        JpaVaultiqSession vaultiqSession = new JpaVaultiqSession();
        vaultiqSession.userId = userId;
        vaultiqSession.deviceFingerPrint = deviceFingerPrint;
        vaultiqSession.createdAt = Instant.now();
        return vaultiqSession;
    }

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

    public String getDeviceFingerPrint() {
        return deviceFingerPrint;
    }

    public void setDeviceFingerPrint(String deviceFingerPrint) {
        this.deviceFingerPrint = deviceFingerPrint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}