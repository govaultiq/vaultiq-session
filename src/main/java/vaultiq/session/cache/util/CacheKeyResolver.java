
package vaultiq.session.cache.util;

/**
 * Utility class for consistent construction of cache keys used throughout the Vaultiq session and blocklist cache infrastructure.
 *
 * <p>
 * All static key-creation methods here produce unique and type-specific keys to avoid collisions in multi-tenant/session-cache environments.
 * These key formats are used to store and retrieve sessions, user-to-sessions mappings, last active timestamps, and blocklist entries
 * for both sessions and users in distributed or in-memory caches.
 * </p>
 *
 * <p>
 * Used broadly in:
 * <ul>
 *   <li>{@link vaultiq.session.cache.service.internal.VaultiqSessionCacheService}</li>
 *   <li>{@link vaultiq.session.cache.service.internal.SessionBlocklistCacheService}</li>
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
     * Returns the cache key for a blocklist entry by session id.
     * Format: {@code blacklist-{sessionId}}
     *
     * Used to store the blocklist (revocation) state for specific session tokens.
     *
     * @param sessionId the session identifier
     * @return the key for use in session blocklist map
     */
    public static String keyForBlacklist(String sessionId) {
        return "blacklist-" + sessionId;
    }

    /**
     * Returns the cache key for all blocklisted sessions for a user.
     * Format: {@code blacklist-by-user-{userId}}
     *
     * Used to fetch/remove the complete list of blocklisted session IDs for a user.
     *
     * @param userId the user identifier
     * @return the key for use in user-to-blocklist map
     */
    public static String keyForBlacklistByUser(String userId) {
        return "blacklist-by-user-" + userId;
    }
}
