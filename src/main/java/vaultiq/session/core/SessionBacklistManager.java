
package vaultiq.session.core;

import java.util.Set;

public interface SessionBacklistManager {

    /**
     * Blocklist (invalidate) all sessions for a given user.
     * Can be used to log out from all devices.
     *
     * @param userId the user identifier
     */
    void blocklistAllSessions(String userId);

    /**
     * Blocklist (invalidate) all sessions except the specified session IDs.
     * Can be used to log out from all devices except, e.g., current device.
     *
     * @param userId the user identifier
     * @param excludedSessionIds session IDs that should NOT be blocklisted
     */
    void blocklistAllSessionsExcept(String userId, String... excludedSessionIds);

    /**
     * Blocklist (invalidate) a specific session by session ID.
     * Can be used to log out from one device.
     *
     * @param sessionId the session identifier
     */
    void blocklistSession(String sessionId);

    /**
     * Check if a session is currently blocklisted.
     *
     * @param sessionId the session identifier
     * @return true if the session is blocklisted, false otherwise
     */
    boolean isSessionBlocklisted(String sessionId);

    /**
     * Get all blocklisted session IDs for a user.
     *
     * @param userId the user identifier
     * @return set of blocklisted session IDs for the user, or empty set if none
     */
    Set<String> getBlocklistedSessions(String userId);
}
