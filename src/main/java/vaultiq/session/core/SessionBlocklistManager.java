package vaultiq.session.core;

import vaultiq.session.core.model.SessionBlocklist;
import vaultiq.session.context.BlocklistContext;

import java.util.List;

/**
 * Manages the blocklisting (invalidation) of Vaultiq sessions.
 * <p>
 * This interface defines the contract for services responsible for marking sessions
 * as invalid or revoked, preventing their further use. Implementations handle the
 * underlying storage and retrieval of blocklisted session information, which can
 * vary based on the library's configured persistence strategy (e.g., cache-ONLY,
 * JPA-ONLY, or a combination JPA_AND_CACHE mode).
 * </p>
 * <p>
 * Consumers of this interface should rely on its defined methods to manage the
 * session blocklist without needing to know the specific persistence mechanism
 * being used by the active implementation.
 * </p>
 */
public interface SessionBlocklistManager {

    /**
     * Adds one or more sessions to the blocklist based on the criteria specified
     * in the provided context.
     * <p>
     * The exact sessions affected and the reason for blocklisting are determined
     * by the {@link BlocklistContext}. Implementations will persist this blocklist
     * information according to their configured strategy (cache, JPA, or both).
     * </p>
     *
     * @param context the context describing the blocklist operation, including
     *                the sessions to blocklist and the reason. Must not be {@code null}.
     */
    void blocklist(BlocklistContext context);

    /**
     * Checks if a specific session is currently present in the blocklist.
     * <p>
     * Implementations will query their underlying persistence store(s) to determine
     * the blocklisted status of the session.
     * </p>
     *
     * @param sessionId the unique identifier of the session to check. Must not be blank.
     * @return {@code true} if the session is blocklisted, {@code false} otherwise.
     */
    boolean isSessionBlocklisted(String sessionId);

    /**
     * Retrieves a list of all sessions that have been blocklisted for a given user.
     * <p>
     * The list includes details about each blocklisted session, such as the session ID,
     * the type of revocation, and when it occurred. Implementations will fetch this
     * information from their configured persistence store(s).
     * </p>
     *
     * @param userId the unique identifier of the user whose blocklisted sessions are
     *               to be retrieved. Must not be blank.
     * @return A {@link List} of {@link SessionBlocklist} objects representing the
     * sessions blocklisted for the user. Returns an empty list if the user
     * has no blocklisted sessions. Never returns {@code null}.
     */
    List<SessionBlocklist> getBlocklistedSessions(String userId);

    /**
     * Clears the blocklist for a specific session or multiple sessions.
     * <p>
     * Implementations will remove the specified session(s) from their underlying persistence store(s).
     * No attempts to clear blocklist will be made if the sessionIds are empty or blank.
     * </p>
     *
     * @param sessionIds an array of unique sessions identifiers to clear. Can be empty. It Can be blank.
     */
    void clearBlocklist(String... sessionIds);
}
