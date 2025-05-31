package vaultiq.session.jpa.session.model;

import jakarta.persistence.*;
import vaultiq.session.domain.model.ClientSession;
import vaultiq.session.jpa.session.repository.ClientSessionEntityRepository;

import java.time.Instant;

/**
 * JPA entity representing a Vaultiq session in the database.
 * <p>
 * This entity is used internally by the {@code vaultiq-session} library's
 * JPA persistence layer to map session data to the {@code session_pool}
 * database table. It holds the persistent state of a user session, including
 * identifiers, timestamps, and revoke status.
 * </p>
 * <p>
 * This class is not intended for direct use by consuming applications;
 * the client-facing model is {@link ClientSession}.
 * </p>
 *
 * @see ClientSession
 * @see ClientSessionEntityRepository
 */
@Entity
@Table(
        name = "session_pool",
        indexes = {
                @Index(name = "idx_user_id_is_revoked", columnList = "user_id, is_revoked"),
                @Index(name = "idx_revoked_at", columnList = "revoked_at")
        }
)
public final class ClientSessionEntity {

    /**
     * The unique identifier for the session, serving as the primary key.
     * Generated automatically as a UUID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id")
    private String sessionId;

    /**
     * The unique identifier of the user associated with this session.
     * This field is mandatory and cannot be updated after creation.
     */
    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    /**
     * A fingerprint or identifier representing the device from which the session originated.
     * This field is mandatory and cannot be updated after creation.
     */
    @Column(name = "device_finger_print", nullable = false, updatable = false)
    private String deviceFingerPrint;

    /**
     * The timestamp (UTC) when this session was created.
     * This field is mandatory and cannot be updated after creation.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Flag indicating whether this session is currently revoked.
     */
    @Column(name = "is_revoked", nullable = false)
    private boolean isRevoked;

    /**
     * The timestamp (UTC) when this session was revoked.
     * This field is nullable, as it's only set when the session is revoked.
     */
    @Column(name = "revoked_at", nullable = true)
    private Instant revokedAt;

    /**
     * Static factory method to create a new {@link ClientSessionEntity} instance
     * with initial mandatory fields set and {@code createdAt} defaulted to {@code Instant.now()}.
     *
     * @param userId            The unique identifier of the user.
     * @param deviceFingerPrint The device fingerprint.
     * @return A new {@link ClientSessionEntity} instance.
     */
    public static ClientSessionEntity create(String userId, String deviceFingerPrint) {
        ClientSessionEntity vaultiqSession = new ClientSessionEntity();
        vaultiqSession.userId = userId;
        vaultiqSession.deviceFingerPrint = deviceFingerPrint;
        vaultiqSession.createdAt = Instant.now();
        // isRevoked defaults to false, revokedAt defaults to null by JPA
        return vaultiqSession;
    }

    // Getters and Setters for JPA access

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

    public boolean isRevoked() {
        return isRevoked;
    }

    public void setRevoked(boolean revoked) {
        isRevoked = revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}