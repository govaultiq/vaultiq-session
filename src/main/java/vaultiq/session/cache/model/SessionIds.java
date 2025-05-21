
package vaultiq.session.cache.model;

import vaultiq.session.cache.service.internal.SessionRevocationCacheService;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A lightweight cache model tracking a collection of session IDs and the time they were last updated.
 *
 * <p>
 * Used primarily by cache service layers (see {@link vaultiq.session.cache.service.internal.VaultiqSessionCacheService}
 * and {@link SessionRevocationCacheService}) to efficiently store,
 * update, and expire mappings of a user's session IDs—either for active sessions or blocklisted ones.
 * Stores all IDs for fast read/write with minimal overhead and enables quick last-modified checks for cache staleness logic.
 * </p>
 *
 * <p>
 * Typical use:
 * <ul>
 *   <li>As the value object for mapping user IDs to sets of session IDs in caches (per-user session pool or revoke).</li>
 *   <li>Maintains a <b>lastUpdated</b> timestamp (epoch millis, UTC) for use in proper staleness and invalidation logic.</li>
 * </ul>
 * </p>
 *
 * <b>Note:</b> Mutators and setters always update the lastUpdated field to the current instant.
 */
public class SessionIds {
    /** The set of associated session IDs (never null may be empty). */
    private Set<String> sessionIds = new HashSet<>();
    /** Epoch millis (UTC) when this collection was last modified. Used for staleness and audit logic. */
    private long lastUpdated; // epoch millis

    /**
     * Returns all session IDs currently held in this collection.
     * @return mutable set of session IDs (never null)
     */
    public Set<String> getSessionIds() {
        return sessionIds;
    }

    /**
     * Replaces the session ID set with the provided one (null-safe), and updates the last-updated timestamp.
     * @param sessionIds a non-null set of session IDs, or empty if none
     */
    public void setSessionIds(Set<String> sessionIds) {
        this.sessionIds = sessionIds != null ? sessionIds : new HashSet<>();
        updatedNow();
    }

    /**
     * Adds a single session ID to this set, updating the last-modified time accordingly.
     * @param sessionId the session identifier to add
     */
    public void addSessionId(String sessionId) {
        sessionIds.add(sessionId);
        updatedNow();
    }

    /**
     * Returns the UTC timestamp (in epoch millis) when this list was last updated.
     * Used for cache staleness checks and auditing.
     * @return the last modification timestamp (epoch millis)
     */
    public long getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Updates the lastUpdated timestamp to the current instant.
     * Internal helper for ensuring consistency on each mutation.
     */
    private void updatedNow() {
        this.lastUpdated = Instant.now().toEpochMilli();
    }
}
