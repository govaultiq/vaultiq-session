
package vaultiq.session.jpa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.SessionBacklistManager;
import vaultiq.session.jpa.service.internal.SessionBlocklistJpaService;

import java.util.Set;

/**
 * JPA-backed implementation of the {@link SessionBacklistManager} interface.
 * <p>
 * This service delegates all session blocklisting operations for the BLOCKLIST model type
 * to {@link SessionBlocklistJpaService}. Blocklisting functionality includes invalidating
 * all sessions, all-except-some, specific sessions, and querying blocklisted state by session ID
 * or collecting all blocklisted session IDs for a user.
 * </p>
 * <p>
 * This bean is activated only if a {@code SessionBlocklistJpaService} bean is present in the context,
 * and the persistence configuration matches JPA mode with a BLOCKLIST model type.
 * </p>
 *
 * @see SessionBacklistManager
 * @see SessionBlocklistJpaService
 */
@Service
@ConditionalOnBean(SessionBlocklistJpaService.class)
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_ONLY, type = ModelType.BLOCKLIST)
public class SessionBlocklistManagerViaJpa implements SessionBacklistManager {

    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistManagerViaJpa.class);

    private final SessionBlocklistJpaService sessionBlocklistJpaService;

    /**
     * Constructs a new {@code SessionBlocklistManagerViaJpa} with the required JPA service dependency.
     *
     * @param sessionBlocklistJpaService the underlying service that actually performs JPA-based operations
     */
    public SessionBlocklistManagerViaJpa(SessionBlocklistJpaService sessionBlocklistJpaService) {
        this.sessionBlocklistJpaService = sessionBlocklistJpaService;
    }

    /**
     * Blocklists (invalidates) all sessions for the specified user.
     * Typically used to log out a user from all devices.
     *
     * @param userId the user identifier for whom all sessions will be blocklisted
     */
    @Override
    public void blocklistAllSessions(String userId) {
        log.debug("Blocklisting all sessions for user '{}'.", userId);
        sessionBlocklistJpaService.blocklistAllSessions(userId);
    }

    /**
     * Blocklists (invalidates) all sessions for the specified user except those with the provided session IDs.
     * This can be used, for example, to log out a user from all devices except the current one.
     *
     * @param userId the user identifier whose sessions to manage
     * @param excludedSessionIds session IDs that will NOT be blocklisted
     */
    @Override
    public void blocklistAllSessionsExcept(String userId, String... excludedSessionIds) {
        log.debug("Blocklisting all sessions for user '{}' except {}.", userId, (Object) excludedSessionIds);
        sessionBlocklistJpaService.blocklistAllSessionsExcept(userId, excludedSessionIds);
    }

    /**
     * Blocklists (invalidates) a single session given its session ID.
     * Used for logging out a particular device.
     *
     * @param sessionId the session ID to be blocklisted
     */
    @Override
    public void blocklistSession(String sessionId) {
        log.debug("Blocklisting session '{}'.", sessionId);
        sessionBlocklistJpaService.blocklistSession(sessionId);
    }

    /**
     * Determines if a given session is currently blocklisted.
     *
     * @param sessionId the session ID to check
     * @return {@code true} if the session is blocklisted, {@code false} otherwise
     */
    @Override
    public boolean isSessionBlocklisted(String sessionId) {
        log.debug("Checking if session '{}' is blocklisted.", sessionId);
        return sessionBlocklistJpaService.isSessionBlocklisted(sessionId);
    }

    /**
     * Retrieves all blocklisted session IDs for the specified user, or an empty set if none.
     *
     * @param userId the user identifier for which to retrieve blocklisted sessions
     * @return a set of blocklisted session IDs, never {@code null}
     */
    @Override
    public Set<String> getBlocklistedSessions(String userId) {
        log.debug("Fetching blocklisted sessions for user '{}'.", userId);
        return sessionBlocklistJpaService.getBlocklistedSessions(userId);
    }
}
