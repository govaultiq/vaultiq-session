
package vaultiq.session.core;

import vaultiq.session.core.model.SessionBlocklist;
import vaultiq.session.core.util.BlocklistContext;

import java.util.List;

public interface SessionBacklistManager {

    /**
     * Blocklist (invalidate) sessions based on the provided context.
     *
     * @param context the context describing the blocklist operation
     */
    void blocklist(BlocklistContext context);

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
     * @return list of blocklisted sessions for the user, or empty set if none
     */
    List<SessionBlocklist> getBlocklistedSessions(String userId);
}
