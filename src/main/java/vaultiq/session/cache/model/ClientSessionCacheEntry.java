
package vaultiq.session.cache.model;

import vaultiq.session.domain.model.ClientSession;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a session entry stored in the Vaultiq session-pool cache.
 * <p>
 * This is a cache-specific modeling of an active session, optimized for serialization and mutation
 * by cache infrastructure, especially by {@link vaultiq.session.cache.service.internal.VaultiqSessionCacheService}.
 * </p>
 * <ul>
 *   <li>Contains session meta (IDs, timestamps, device fingerprint), as well as blocklisting info.</li>
 *   <li>Equals/hashCode use only the session ID—suitable for caching and set operations.</li>
 *   <li>Does <b>not</b> contain security tokens, credentials, or sensitive user data.</li>
 * </ul>
 *
 * <p>
 * Usage:
 * <ol>
 *   <li>Created via the static {@link #create} factory for new sessions or {@link #copy} for copying an existing model.</li>
 *   <li>States are toggled as needed (e.g., {@link #revoke()} ) during invalidation workflows.</li>
 * </ol>
 * </p>
 */
public final class ClientSessionCacheEntry implements Serializable {
    /**
     * The unique session ID for this cached session.
     */
    private String sessionId;
    /**
     * The associated user ID.
     */
    private String userId;
    /**
     * Device fingerprint—used for device-awareness.
     */
    private String deviceFingerPrint;
    /**
     * Creation timestamp (UTC, cache server's time).
     */
    private Instant createdAt;
    /**
     * If true, the session has been blocklisted or invalidated.
     */
    private boolean isRevoked;
    /**
     * Blocklisting timestamp, if ever blocked.
     */
    private Instant revokedAt;

    /**
     * No-arg constructor for serialization/deserialization only.
     */
    private ClientSessionCacheEntry() {
        // Avoiding Direct Instantiation
    }

    /**
     * Creates a new session cache entry for a user and device.
     * Uses a random UUID for sessionId and the current time for createdAt.
     *
     * @param userId            user identifier
     * @param deviceFingerPrint fingerprint from {@code DeviceFingerprintGenerator}
     * @return a new session cache entry (not blocklisted)
     */
    public static ClientSessionCacheEntry create(String userId, String deviceFingerPrint) {
        ClientSessionCacheEntry vaultiqSession = new ClientSessionCacheEntry();
        vaultiqSession.sessionId = UUID.randomUUID().toString();
        vaultiqSession.userId = userId;
        vaultiqSession.deviceFingerPrint = deviceFingerPrint;
        vaultiqSession.createdAt = Instant.now();
        return vaultiqSession;
    }

    /**
     * Creates a cache entry by copying data from an existing {@link ClientSession} model.
     * Used for copying/restoring sessions into the cache.
     *
     * @param source the model session
     * @return cache entry with same IDs, fingerprint, and created time
     */
    public static ClientSessionCacheEntry copy(ClientSession source) {
        ClientSessionCacheEntry cacheEntry = new ClientSessionCacheEntry();
        cacheEntry.sessionId = source.getSessionId();
        cacheEntry.userId = source.getUserId();
        cacheEntry.deviceFingerPrint = source.getDeviceFingerPrint();
        cacheEntry.createdAt = source.getCreatedAt();
        cacheEntry.revokedAt = source.getRevokedAt();
        cacheEntry.isRevoked = source.isRevoked();
        return cacheEntry;
    }

    /**
     * Marks this cache entry as blocklisted and records the current time as block/invalidated.
     */
    public void revoke() {
        this.isRevoked = true;
        this.revokedAt = Instant.now();
    }

    /**
     * @return this entry's session ID string
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * @return the related user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @return the stored device fingerprint
     */
    public String getDeviceFingerPrint() {
        return deviceFingerPrint;
    }

    /**
     * @return time this session entry was created/added to cache
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * @return true if this session has been blocked/invalidated
     */
    public boolean isRevoked() {
        return isRevoked;
    }

    /**
     * @return the time of blocklisting, or null if never blocked
     */
    public Instant getRevokedAt() {
        return revokedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientSessionCacheEntry that = (ClientSessionCacheEntry) o;

        return sessionId.equals(that.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }

    @Override
    public String toString() {
        return "ClientSession{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", createdAt=" + createdAt +
                ", isRevoked=" + isRevoked +
                ", revokedAt=" + revokedAt +
                '}';
    }
}
