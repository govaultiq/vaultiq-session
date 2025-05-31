package vaultiq.session.domain.contracts.internal;

import jakarta.servlet.http.HttpServletRequest;
import vaultiq.session.domain.model.ClientSession;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Core interface for managing Vaultiq sessions.
 * <p>
 * This interface defines the standard operations for creating, retrieving,
 * deleting, and querying {@link ClientSession} objects within the Vaultiq
 * session tracking library. Implementations of this interface provide the
 * underlying persistence logic (e.g., cache-based, JPA-based, or a combination).
 * </p>
 * <p>
 * Different implementations may be active depending on the library's configuration
 * and the application's environment, often managed via Spring's conditional
 * mechanisms (e.g., {@link vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence}).
 * </p>
 */
public interface SessionManager {

    /**
     * Creates a new Vaultiq session for the specified user.
     * <p>
     * The session details are typically derived from the user ID and the incoming
     * HTTP request. The newly created session is persisted according to the
     * configured persistence strategy.
     * </p>
     *
     * @param userId  The unique identifier of the user for whom the session is created.
     * @param request The incoming {@link HttpServletRequest} containing client details.
     * @return The newly created {@link ClientSession} object.
     */
    ClientSession createSession(String userId, HttpServletRequest request);

    /**
     * Retrieves an existing Vaultiq session by its unique session ID.
     * <p>
     * The session is looked up using the configured persistence strategy.
     * </p>
     *
     * @param sessionId The unique identifier of the session to retrieve.
     * @return The {@link ClientSession} object if found, or {@code null} if no session exists with the given ID.
     */
    ClientSession getSession(String sessionId);

    /**
     * Retrieves the device fingerprint associated with a specific session ID.
     * <p>
     *
     * @param sessionId The unique identifier of the session.
     * @return The device fingerprint associated with the session. Returns {@code null} if no session exists with the given ID.
     */
    Optional<String> getSessionFingerprint(String sessionId);

    /**
     * Deletes a Vaultiq session based on its unique session ID.
     * <p>
     * The session is removed from the configured persistence store(s).
     * </p>
     *
     * @param sessionId The unique identifier of the session to delete.
     */
    void deleteSession(String sessionId);

    /**
     * Deletes multiple Vaultiq sessions by their unique session IDs.
     * <p>
     *
     * @param sessionIds A set of unique session IDs to delete.
     */
    void deleteAllSessions(Set<String> sessionIds);

    /**
     * Retrieves all active Vaultiq sessions associated with a specific user ID.
     * <p>
     * The sessions are retrieved from the configured persistence store(s).
     * </p>
     *
     * @param userId The unique identifier of the user whose sessions are to be retrieved.
     * @return A {@link List} of {@link ClientSession} objects for the user. Returns an empty list if no sessions are found.
     */
    List<ClientSession> getSessionsByUser(String userId);

    /**
     * Retrieves all active Vaultiq sessions for a user.
     * <p>
     *
     * @param userId The unique identifier of the user whose sessions are to be retrieved.
     * @return A count of active sessions for the user. Returns 0 if no sessions are found.
     */
    List<ClientSession> getActiveSessionsByUser(String userId);

    /**
     * Counts the total number of active Vaultiq sessions associated with a specific user ID.
     * <p>
     * The count is determined based on the sessions available in the configured persistence store(s).
     * </p>
     *
     * @param userId The unique identifier of the user whose sessions are to be counted.
     * @return The total number of active sessions for the user.
     */
    int totalUserSessions(String userId);

}