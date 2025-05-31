package vaultiq.session.cache.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.util.CacheHelper;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.domain.contracts.internal.SessionManager;
import vaultiq.session.domain.model.ModelType;
import vaultiq.session.domain.model.RevocationRequest;
import vaultiq.session.domain.model.ClientSession;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Service for managing revoked session IDs within the application's cache layer.</p>
 *
 * <p>This service provides quick lookup and management of session IDs that have been
 * explicitly marked as revoked, ensuring they cannot be used for active sessions.
 * It caches a simple boolean flag ({@code true}) for each revoked session ID.</p>
 *
 * <p>Crucially, this service can independently resolve which session IDs to revoke
 * based on a {@link RevocationRequest} by interacting directly with the
 * {@link SessionManager} to get active session data. This makes it a robust
 * component even if other cache layers are not fully operational or available.</p>
 *
 * <p>The service is conditionally active: it will only be instantiated by Spring
 * if the application's persistence method is {@link VaultiqPersistenceMethod#USE_CACHE}
 * for {@link ModelType#REVOKE} data.</p>
 *
 * @see CacheHelper
 * @see SessionManager
 * @see RevocationRequest
 * @see ModelType
 */
@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.REVOKE)
public class RevokedSIdCacheService {
    private static final Logger log = LoggerFactory.getLogger(RevokedSIdCacheService.class);

    private final CacheHelper revokedSIdsCache;
    private final SessionManager sessionManager;

    /**
     * Constructs the {@code RevokedSIdCacheService}.
     * Spring automatically injects the {@link CacheHelper} bean qualified for revoked SIDs
     * and the {@link SessionManager} for resolving active session IDs.
     *
     * @param revokedSIdsCache The {@link CacheHelper} instance specifically configured
     * for managing revoked session IDs in the cache. Cannot be null.
     * @param sessionManager   The {@link SessionManager} to retrieve active session data
     * when resolving session IDs for revocation requests. Cannot be null.
     */
    public RevokedSIdCacheService(
            @Qualifier(CacheHelper.BeanNames.REVOKED_SIDS_CACHE_HELPER)
            CacheHelper revokedSIdsCache,
            SessionManager sessionManager
    ) {
        this.revokedSIdsCache = Objects.requireNonNull(revokedSIdsCache,
                "RevokedSIdsCacheHelper bean not found or is null.");
        this.sessionManager = Objects.requireNonNull(sessionManager, "SessionManager bean not found or is null.");
    }

    /**
     * Marks a single session ID as revoked by caching it.
     * A boolean {@code true} value is stored to indicate its revoked status.
     * No action is taken if the session ID is null or blank.
     *
     * @param sessionId The unique ID of the session to revoke. Cannot be null or blank.
     */
    public void revoke(String sessionId) {
        if (isSessionIdNotValid(sessionId)) return;
        log.debug("Revoking single session ID: {}", sessionId);
        revokedSIdsCache.cache(sessionId, true);
    }

    /**
     * Marks multiple session IDs as revoked by caching them individually.
     * A boolean {@code true} value is stored for each session ID to indicate its revoked status.
     * No action is taken if the provided set of session IDs is null or empty.
     *
     * @param sessionIdSet The set of session IDs to mark as revoked. Cannot be null or empty.
     */
    public void revoke(Set<String> sessionIdSet) {
        if (sessionIdSet == null || sessionIdSet.isEmpty()) {
            log.warn("Attempt to revoke multiple session IDs with null or empty set. No action will be taken.");
            return;
        }
        log.debug("Revoking {} session IDs in batch.", sessionIdSet.size());
        sessionIdSet.forEach(this::revoke);
    }

    /**
     * Initiates a revocation operation based on a {@link RevocationRequest}.
     * This method resolves the target session IDs from the request (e.g., all sessions for a user,
     * or specific sessions with exclusions) by querying the {@link SessionManager},
     * and then marks those resolved session IDs as revoked in the cache.
     *
     * @param revocationRequest The {@link RevocationRequest} describing the revoke operation. Cannot be null.
     * @throws NullPointerException if the {@code revocationRequest} is null or its identifier is null when required.
     */
    public void revoke(RevocationRequest revocationRequest) {
        Objects.requireNonNull(revocationRequest, "RevocationRequest cannot be null.");
        log.debug("Processing revocation request of type: {}", revocationRequest.getRevocationType());
        Set<String> sessionIdSet = resolveSessionIdsToRevoke(revocationRequest);
        revoke(sessionIdSet); // Delegate to the batch revoke method
    }

    /**
     * Internal helper method to resolve the set of session IDs to revoke based on the {@link RevocationRequest}.
     * It uses the {@link SessionManager} to get active session data when needed (e.g., for LOGOUT_ALL).
     *
     * @param request The {@link RevocationRequest}.
     * @return A {@link Set} of session IDs to be revoked.
     * @throws NullPointerException if the request's identifier is null for LOGOUT or LOGOUT_ALL types.
     */
    private Set<String> resolveSessionIdsToRevoke(RevocationRequest request) {
        return switch (request.getRevocationType()) {
            case LOGOUT -> {
                Objects.requireNonNull(request.getIdentifier(), "Session ID for LOGOUT revocation cannot be null.");
                yield Set.of(request.getIdentifier());
            }
            case LOGOUT_ALL -> getActiveSessionIdsForUser(request);
            case LOGOUT_WITH_EXCLUSION -> {
                Set<String> allActiveUserSessions = getActiveSessionIdsForUser(request);
                Set<String> excludedIds = Objects.requireNonNullElse(request.getExcludedSessionIds(), Collections.emptySet());
                yield allActiveUserSessions.stream()
                        .filter(id -> !excludedIds.contains(id))
                        .collect(Collectors.toSet());
            }
        };
    }

    /**
     * Internal helper method to retrieve all active session IDs for a user from the {@link SessionManager}.
     *
     * @param request The {@link RevocationRequest} containing the user identifier.
     * @return A {@link Set} of active session IDs for the user.
     * @throws NullPointerException if the request's identifier (user ID) is null.
     */
    private Set<String> getActiveSessionIdsForUser(RevocationRequest request) {
        String userId = Objects.requireNonNull(request.getIdentifier(), "User ID for revocation request cannot be null.");
        List<ClientSession> activeSessions = sessionManager.getActiveSessionsByUser(userId);
        if (activeSessions == null || activeSessions.isEmpty()) {
            log.debug("No active sessions found for user '{}' during revocation request.", userId);
            return Collections.emptySet();
        }
        return activeSessions.stream().map(ClientSession::getSessionId).collect(Collectors.toSet());
    }

    /**
     * Checks if a given session ID is currently marked as revoked in the cache.
     *
     * @param sessionId The unique ID of the session to check. Cannot be null or blank.
     * @return {@code true} if the session ID is found in the revoked cache, {@code false} otherwise.
     */
    public boolean isSessionRevoked(String sessionId) {
        if (isSessionIdNotValid(sessionId)) return false;
        log.debug("Checking if session ID {} is revoked.", sessionId);
        return Boolean.TRUE.equals(revokedSIdsCache.get(sessionId, Boolean.class));
    }

    /**
     * Removes a specific revoked session ID from the cache, effectively un-revoking it.
     * No action is taken if the session ID is not found in the cache or is invalid.
     *
     * @param sessionId The unique ID of the session to evict from the revoked list. Cannot be null or blank.
     */
    public void evictRevokedSId(String sessionId) {
        if (isSessionIdNotValid(sessionId)) return;
        log.debug("Evicting session ID: {}", sessionId);
        revokedSIdsCache.evict(sessionId);
    }

    /**
     * Removes multiple revoked session IDs from the cache in a batch operation.
     * No action is taken if the provided set of session IDs is null or empty.
     *
     * @param sessionIds A {@link Set} of session IDs to evict from the revoked list. Can be null or empty.
     */
    public void evictRevokedSids(Set<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            log.warn("The SessionId set is null or empty for eviction; No action will be taken.");
            return;
        }
        log.debug("Evicting multiple revoked session IDs: {}", sessionIds.size());
        sessionIds.forEach(this::evictRevokedSId); // Delegate to single eviction method
    }

    /**
     * Internal helper method to check if a session ID is null or blank.
     * Logs a warning if the session ID is invalid.
     *
     * @param sessionId The session ID to validate.
     * @return {@code true} if the session ID is null or blank, {@code false} otherwise.
     */
    private boolean isSessionIdNotValid(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("sessionId is null or blank; No action will be taken.");
            return true;
        }
        return false;
    }
}