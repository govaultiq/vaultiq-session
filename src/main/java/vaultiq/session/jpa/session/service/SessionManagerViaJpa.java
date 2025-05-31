package vaultiq.session.jpa.session.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vaultiq.session.core.service.SessionManager;
import vaultiq.session.model.ClientSession;
import vaultiq.session.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.jpa.session.service.internal.ClientSessionEntityService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JPA-only implementation of the {@link SessionManager} interface.
 * <p>
 * This service provides the core session management operations (create, get,
 * delete, list, count) by delegating all calls to an underlying JPA-based
 * {@link ClientSessionEntityService}. It is designed to be active specifically
 * when the Vaultiq session library is configured to use JPA as the *only*
 * persistence method for {@link ModelType#SESSION}
 * data models.
 * </p>
 * <p>
 * This bean is automatically configured by Spring when a {@link ClientSessionEntityService}
 * bean is available and the persistence configuration matches
 * {@link VaultiqPersistenceMode#JPA_ONLY} for the relevant model types,
 * as defined by {@link ConditionalOnVaultiqPersistence}.
 * </p>
 *
 * @see SessionManager
 * @see ClientSessionEntityService
 * @see ConditionalOnVaultiqPersistence
 * @see VaultiqPersistenceMode#JPA_ONLY
 * @see ModelType#SESSION
 */
@Service
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_ONLY, type = ModelType.SESSION)
public class SessionManagerViaJpa implements SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManagerViaJpa.class);

    private final ClientSessionEntityService sessionService;

    /**
     * Constructs a new {@code SessionManagerViaJpa} with the required
     * JPA-based session service dependency.
     *
     * @param sessionService The underlying service that performs the actual JPA operations.
     */
    public SessionManagerViaJpa(ClientSessionEntityService sessionService) {
        this.sessionService = sessionService;
        log.info("SessionManager initialized; Persistence via - JPA_ONLY.");
    }

    /**
     * @inheritDoc <p>Delegates session creation to the underlying JPA service.</p>
     */
    @Override
    public ClientSession createSession(String userId, HttpServletRequest request) {
        log.debug("Creating session for user '{}' via JPA.", userId);
        return sessionService.create(userId, request);
    }

    /**
     * @inheritDoc <p>Delegates session retrieval by ID to the underlying JPA service.</p>
     */
    @Override
    public ClientSession getSession(String sessionId) {
        log.debug("Retrieving session '{}' via JPA.", sessionId);
        return sessionService.get(sessionId);
    }

    /**
     * @param sessionId The unique identifier of the session.
     * @return Optional String representing the device fingerprint. Returns {@code null} if no session exists with the given ID in the database.
     * @inheritDoc <p>Delegates session fingerprint retrieval by ID to the underlying JPA service.</p>
     */
    @Override
    public Optional<String> getSessionFingerprint(String sessionId) {
        log.debug("Retrieving session fingerprint for session '{}' via JPA.", sessionId);
        return sessionService.getSessionFingerprint(sessionId);
    }

    /**
     * @inheritDoc <p>Delegates session deletion by ID to the underlying JPA service.</p>
     */
    @Override
    public void deleteSession(String sessionId) {
        log.debug("Deleting session '{}' via JPA.", sessionId);
        sessionService.delete(sessionId);
    }

    /**
     * @param sessionIds A set of unique session IDs to delete.
     * @inheritDoc <p>Delegates deletion of all sessions by ID to the underlying JPA service.</p>
     */
    @Override
    public void deleteAllSessions(Set<String> sessionIds) {
        log.debug("Deleting all sessions via JPA.");
        sessionService.deleteAllSessions(sessionIds);
    }

    /**
     * @inheritDoc <p>Delegates listing sessions by user ID to the underlying JPA service.</p>
     */
    @Override
    public List<ClientSession> getSessionsByUser(String userId) {
        log.debug("Listing sessions for user '{}' via JPA.", userId);
        return sessionService.list(userId);
    }

    /**
     * @param userId The unique identifier of the user whose sessions are to be retrieved.
     * @return the active sessions for user.
     * @inheritDoc <p>Delegates counting active sessions by user ID to the underlying JPA service.</p>
     */
    @Override
    public List<ClientSession> getActiveSessionsByUser(String userId) {
        log.debug("Getting all active sessions for user '{}' via JPA.", userId);
        return sessionService.getActiveSessionsByUser(userId);
    }

    /**
     * @inheritDoc <p>Delegates counting sessions by user ID to the underlying JPA service.</p>
     */
    @Override
    public int totalUserSessions(String userId) {
        log.debug("Counting sessions for user '{}' via JPA.", userId);
        return sessionService.count(userId);
    }
}
