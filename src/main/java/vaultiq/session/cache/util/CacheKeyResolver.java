
package vaultiq.session.cache.util;

import vaultiq.session.cache.service.internal.SessionRevocationCacheService;

/**
 * Utility class for consistent construction of cache keys used throughout the Vaultiq session and revoke cache infrastructure.
 *
 * <p>
 * All static key-creation methods here produce unique and type-specific keys to avoid collisions in multi-tenant/session-cache environments.
 * These key formats are used to store and retrieve sessions, user-to-sessions mappings, last active timestamps, and revoke entries
 * for both sessions and users in distributed or in-memory caches.
 * </p>
 *
 * <p>
 * Used broadly in:
 * <ul>
 *   <li>{@link vaultiq.session.cache.service.internal.VaultiqSessionCacheService}</li>
 *   <li>{@link SessionRevocationCacheService}</li>
 * </ul>
 * </p>
 *
 * <b>Note:</b> Class is final with a private constructor to enforce usage only via static methods.
 */
public final class CacheKeyResolver {

    /** Hidden utility constructor to prevent external instantiation. */
    private CacheKeyResolver() {
        // avoiding external instantiation
    }

    /**
     * Returns the cache key for a specific session in the session pool.
     * Format: {@code session-pool-{sessionId}}
     *
     * @param sessionId the session identifier
     * @return the complete cache key for the session pool map
     */
    public static String keyForSession(String sessionId) {
        return "session-pool-" + sessionId;
    }

    /**
     * Returns the cache key for the mapping of a user's ID to their session ID set.
     * Format: {@code user-sessions-{userId}}
     *
     * Used as the key in caches for quick lookup of all active session ids for a user.
     *
     * @param userId the user identifier
     * @return the key for use in user-to-sessions cache
     */
    public static String keyForUserSessionMapping(String userId) {
        return "user-sessions-" + userId;
    }

    /**
     * Returns the cache key for storing/retrieving the last active timestamp of a user.
     * Format: {@code last-active-{userId}}
     *
     * Used for session activity or idle-timeout tracking.
     *
     * @param userId the user identifier
     * @return the key for use in last-active-timestamp map
     */
    public static String keyForLastActiveTimestamp(String userId) {
        return "last-active-" + userId;
    }

    /**
     * Returns the cache key for a revoke entry by session id.
     * Format: {@code revocation-{sessionId}}
     *
     * Used to store the revoke (revocation) state for specific session tokens.
     *
     * @param sessionId the session identifier
     * @return the key for use in a session revoke map
     */
    public static String keyForRevocation(String sessionId) {
        return "revocation-" + sessionId;
    }

    /**
     * Returns the cache key for all revoked sessions for a user.
     * Format: {@code revocation-by-user-{userId}}
     *
     * Used to fetch/remove the complete list of revoked session IDs for a user.
     *
     * @param userId the user identifier
     * @return the key for use in a user-to-revoke map
     */
    public static String keyForRevocationByUser(String userId) {
        return "revocation-by-user-" + userId;
    }
}
