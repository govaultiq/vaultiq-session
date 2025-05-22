package vaultiq.session.jpa.session.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.jpa.session.service.internal.VaultiqSessionEntityService;

import java.util.List;
import java.util.Set;

/**
 * JPA-only implementation of the {@link VaultiqSessionManager} interface.
 * <p>
 * This service provides the core session management operations (create, get,
 * delete, list, count) by delegating all calls to an underlying JPA-based
 * {@link VaultiqSessionEntityService}. It is designed to be active specifically
 * when the Vaultiq session library is configured to use JPA as the *only*
 * persistence method for {@link ModelType#SESSION}
 * data models.
 * </p>
 * <p>
 * This bean is automatically configured by Spring when a {@link VaultiqSessionEntityService}
 * bean is available and the persistence configuration matches
 * {@link VaultiqPersistenceMode#JPA_ONLY} for the relevant model types,
 * as defined by {@link ConditionalOnVaultiqPersistence}.
 * </p>
 *
 * @see VaultiqSessionManager
 * @see VaultiqSessionEntityService
 * @see ConditionalOnVaultiqPersistence
 * @see VaultiqPersistenceMode#JPA_ONLY
 * @see ModelType#SESSION
 */
@Service
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_ONLY, type = ModelType.SESSION)
public class VaultiqSessionManagerViaJpa implements VaultiqSessionManager {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpa.class);

    private final VaultiqSessionEntityService sessionService;

    /**
     * Constructs a new {@code VaultiqSessionManagerViaJpa} with the required
     * JPA-based session service dependency.
     *
     * @param sessionService The underlying service that performs the actual JPA operations.
     */
    public VaultiqSessionManagerViaJpa(VaultiqSessionEntityService sessionService) {
        this.sessionService = sessionService;
        log.info("VaultiqSessionManager initialized; Persistence via - JPA_ONLY.");
    }

    /**
     * @inheritDoc <p>Delegates session creation to the underlying JPA service.</p>
     */
    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        log.debug("Creating session for user '{}' via JPA.", userId);
        return sessionService.create(userId, request);
    }

    /**
     * @inheritDoc <p>Delegates session retrieval by ID to the underlying JPA service.</p>
     */
    @Override
    public VaultiqSession getSession(String sessionId) {
        log.debug("Retrieving session '{}' via JPA.", sessionId);
        return sessionService.get(sessionId);
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
    public List<VaultiqSession> getSessionsByUser(String userId) {
        log.debug("Listing sessions for user '{}' via JPA.", userId);
        return sessionService.list(userId);
    }

    /**
     * @inheritDoc <p>Delegates counting active sessions by user ID to the underlying JPA service.</p>
     * @param userId The unique identifier of the user whose sessions are to be retrieved.
     * @return the active sessions for user.
     */
    @Override
    public List<VaultiqSession> getActiveSessionsByUser(String userId) {
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
