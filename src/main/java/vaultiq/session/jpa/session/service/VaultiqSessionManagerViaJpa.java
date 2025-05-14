package vaultiq.session.jpa.session.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.model.VaultiqSession;
import vaultiq.session.core.VaultiqSessionManager;
import vaultiq.session.jpa.session.service.internal.VaultiqSessionService;

import java.util.List;

/**
 * JPA-only implementation of the {@link VaultiqSessionManager} interface.
 * <p>
 * This service provides the core session management operations (create, get,
 * delete, list, count) by delegating all calls to an underlying JPA-based
 * {@link VaultiqSessionService}. It is designed to be active specifically
 * when the Vaultiq session library is configured to use JPA as the *only*
 * persistence method for {@link ModelType#SESSION} and {@link ModelType#USER_SESSION_MAPPING}
 * data models.
 * </p>
 * <p>
 * This bean is automatically configured by Spring when a {@link VaultiqSessionService}
 * bean is available and the persistence configuration matches
 * {@link VaultiqPersistenceMode#JPA_ONLY} for the relevant model types,
 * as defined by {@link ConditionalOnVaultiqPersistence}.
 * </p>
 *
 * @see VaultiqSessionManager
 * @see VaultiqSessionService
 * @see ConditionalOnVaultiqPersistence
 * @see VaultiqPersistenceMode#JPA_ONLY
 * @see ModelType#SESSION
 * @see ModelType#USER_SESSION_MAPPING
 */
@Service
@ConditionalOnBean(VaultiqSessionService.class)
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_ONLY, type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING})
public class VaultiqSessionManagerViaJpa implements VaultiqSessionManager {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionManagerViaJpa.class);

    private final VaultiqSessionService sessionService;

    /**
     * Constructs a new {@code VaultiqSessionManagerViaJpa} with the required
     * JPA-based session service dependency.
     *
     * @param sessionService The underlying service that performs the actual JPA operations.
     */
    public VaultiqSessionManagerViaJpa(VaultiqSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * @inheritDoc
     * <p>Delegates session creation to the underlying JPA service.</p>
     */
    @Override
    public VaultiqSession createSession(String userId, HttpServletRequest request) {
        log.debug("Creating session for user '{}' via JPA.", userId);
        return sessionService.create(userId, request);
    }

    /**
     * @inheritDoc
     * <p>Delegates session retrieval by ID to the underlying JPA service.</p>
     */
    @Override
    public VaultiqSession getSession(String sessionId) {
        log.debug("Retrieving session '{}' via JPA.", sessionId);
        return sessionService.get(sessionId);
    }

    /**
     * @inheritDoc
     * <p>Delegates session deletion by ID to the underlying JPA service.</p>
     */
    @Override
    public void deleteSession(String sessionId) {
        log.debug("Deleting session '{}' via JPA.", sessionId);
        sessionService.delete(sessionId);
    }

    /**
     * @inheritDoc
     * <p>Delegates listing sessions by user ID to the underlying JPA service.</p>
     */
    @Override
    public List<VaultiqSession> getSessionsByUser(String userId) {
        log.debug("Listing sessions for user '{}' via JPA.", userId);
        return sessionService.list(userId);
    }

    /**
     * @inheritDoc
     * <p>Delegates counting sessions by user ID to the underlying JPA service.</p>
     */
    @Override
    public int totalUserSessions(String userId) {
        log.debug("Counting sessions for user '{}' via JPA.", userId);
        return sessionService.count(userId);
    }
}
