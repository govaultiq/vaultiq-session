package vaultiq.session.cache.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vaultiq.session.cache.util.CacheHelper;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.core.model.ModelType;

import java.util.Objects;
import java.util.Set;

/**
 * <p>Service for managing revoked session IDs within the application's cache layer.</p>
 *
 * <p>This service provides quick lookup and management of session IDs that have been
 * explicitly marked as revoked, ensuring they cannot be used for active sessions.
 * It caches a simple boolean flag ({@code true}) for each revoked session ID.</p>
 *
 * <p>The service is conditionally active: it will only be instantiated by Spring
 * if the application's persistence method is {@link VaultiqPersistenceMethod#USE_CACHE}
 * for {@link ModelType#REVOKE} data.</p>
 *
 * @see CacheHelper
 * @see ModelType
 */
@Service
@ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.REVOKE)
public class RevokedSIdCacheService {
    private static final Logger log = LoggerFactory.getLogger(RevokedSIdCacheService.class);

    private final CacheHelper revokedSIdsCache;

    /**
     * Constructs the {@code RevokedSIdCacheService}.
     * Spring automatically injects the {@link CacheHelper} bean qualified for revoked SIDs.
     *
     * @param revokedSIdsCache The {@link CacheHelper} instance specifically configured
     * for managing revoked session IDs in the cache. Cannot be null.
     */
    public RevokedSIdCacheService(
            @Qualifier(CacheHelper.BeanNames.REVOKED_SIDS_CACHE_HELPER)
            CacheHelper revokedSIdsCache
    ) {
        this.revokedSIdsCache = Objects.requireNonNull(revokedSIdsCache,
                "RevokedSIdsCacheHelper bean not found or is null.");
    }

    /**
     * Marks a session ID as revoked by caching it.
     * A boolean {@code true} value is stored to indicate its revoked status.
     *
     * @param sessionId The unique ID of the session to revoke. Cannot be null or blank.
     */
    public void revokeSId(String sessionId) {
        if(isSessionIdNotValid(sessionId)) return;
        log.debug("Revoking session ID: {}", sessionId);
        revokedSIdsCache.cache(sessionId, true);
    }

    /**
     * Checks if a given session ID is currently marked as revoked in the cache.
     *
     * @param sessionId The unique ID of the session to check. Cannot be null or blank.
     * @return {@code true} if the session ID is found in the revoked cache, {@code false} otherwise.
     */
    public boolean isSessionRevoked(String sessionId) {
        if(isSessionIdNotValid(sessionId)) return false;
        log.debug("Checking if session ID {} is revoked.", sessionId);
        return Boolean.TRUE.equals(revokedSIdsCache.get(sessionId, Boolean.class));
    }

    /**
     * Removes a specific revoked session ID from the cache, effectively un-revoking it.
     * No action is taken if the session ID is not found in the cache.
     *
     * @param sessionId The unique ID of the session to evict from the revoked list. Cannot be null or blank.
     */
    public void evictRevokedSId(String sessionId) {
        if(isSessionIdNotValid(sessionId)) return;
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
        if(sessionIds == null || sessionIds.isEmpty()) {
            log.warn("The SessionId set is null or empty; No action will be taken.");
            return;
        }
        log.debug("Evicting multiple revoked session IDs: {}", sessionIds);
        revokedSIdsCache.evictAll(sessionIds);
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