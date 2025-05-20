
package vaultiq.session.jpa.blocklist.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMode;
import vaultiq.session.core.SessionBlocklistManager;
import vaultiq.session.core.model.SessionBlocklist;
import vaultiq.session.jpa.blocklist.model.SessionBlocklistEntity;
import vaultiq.session.jpa.blocklist.service.internal.SessionBlocklistEntityService;
import vaultiq.session.context.BlocklistContext;

import java.util.*;

/**
 * JPA-backed implementation of the {@link SessionBlocklistManager} interface.
 * <p>
 * This service delegates all session blocklisting operations for the BLOCKLIST model type
 * to {@link SessionBlocklistEntityService}. Blocklisting functionality includes invalidating
 * all sessions, all-except-some, specific sessions, and querying blocklisted state by session ID
 * or collecting all blocklisted session IDs for a user.
 * </p>
 * <p>
 * This bean is activated only if a {@code SessionBlocklistEntityService} bean is present in the context,
 * and the persistence configuration matches JPA mode with a BLOCKLIST model type.
 * </p>
 *
 * @see SessionBlocklistManager
 * @see SessionBlocklistEntityService
 */
@Service
@ConditionalOnVaultiqPersistence(mode = VaultiqPersistenceMode.JPA_ONLY, type = ModelType.BLOCKLIST)
public class SessionBlocklistManagerViaJpa implements SessionBlocklistManager {

    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistManagerViaJpa.class);

    private final SessionBlocklistEntityService sessionBlocklistEntityService;

    /**
     * Constructs a new {@code SessionBlocklistManagerViaJpa} with the required JPA service dependency.
     *
     * @param sessionBlocklistEntityService the underlying service that actually performs JPA-based operations
     */
    public SessionBlocklistManagerViaJpa(SessionBlocklistEntityService sessionBlocklistEntityService) {
        this.sessionBlocklistEntityService = sessionBlocklistEntityService;
    }

    /**
     * Blocklist (invalidate) sessions based on the provided context.
     *
     * @param context the context describing the blocklist operation
     */
    @Override
    public void blocklist(BlocklistContext context) {
        sessionBlocklistEntityService.blocklist(context);
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
        return sessionBlocklistEntityService.isSessionBlocklisted(sessionId);
    }

    /**
     * Retrieves all blocklisted session IDs for the specified user, or an empty set if none.
     *
     * @param userId the user identifier for which to retrieve blocklisted sessions
     * @return a list of blocklisted sessions, never {@code null}
     */
    @Override
    public List<SessionBlocklist> getBlocklistedSessions(String userId) {
        log.debug("Fetching blocklisted sessions for user '{}'.", userId);
        return sessionBlocklistEntityService.getBlocklistedSessions(userId)
                .stream()
                .map(this::toSessionBlocklist)
                .toList();
    }

    /**
     * Clears the blocklist for a specific session or multiple sessions.
     *
     * @param sessionIds an array of unique sessions identifiers to clear. Can be empty. It Can be blank.
     */
    @Override
    public void clearBlocklist(String... sessionIds) {
        log.debug("Attempting to clear blocklist for {} sessionIds.", sessionIds.length);
        sessionBlocklistEntityService.clearBlocklist(sessionIds);
    }

    /**
     * Helper method to convert a JPA entity to a SessionBlocklist object.
     *
     * @param sessionBlocklistEntity the JPA entity to convert
     * @return the converted SessionBlocklist object
     */
    private SessionBlocklist toSessionBlocklist(SessionBlocklistEntity sessionBlocklistEntity) {
        return SessionBlocklist.builder()
                .sessionId(sessionBlocklistEntity.getSessionId())
                .userId(sessionBlocklistEntity.getUserId())
                .revocationType(sessionBlocklistEntity.getRevocationType())
                .note(sessionBlocklistEntity.getNote())
                .triggeredBy(sessionBlocklistEntity.getTriggeredBy())
                .blocklistedAt(sessionBlocklistEntity.getBlocklistedAt())
                .build();
    }
}
