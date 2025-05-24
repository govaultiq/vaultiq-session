package vaultiq.session.cache.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vaultiq.session.cache.util.CacheHelper;
import vaultiq.session.cache.util.CacheType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistenceRequirement;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;

/**
 * <p>
 * Auto-registration configuration class for {@link CacheHelper} beans.
 * This class is responsible for defining and registering specific {@code CacheHelper} instances
 * into the Spring application context, making them available for injection throughout the application.
 * </p>
 *
 * <p>
 * Each {@code CacheHelper} bean is named using constants from {@link CacheHelper.BeanNames}
 * for consistent and compile-time safe referencing.
 * </p>
 *
 * <p>
 * This configuration is conditionally enabled by {@code @ConditionalOnVaultiqPersistenceRequirement(VaultiqPersistenceMethod.USE_CACHE)},
 * meaning these {@code CacheHelper} beans will only be created and registered if the
 * application's persistence requirement is explicitly set to use caching.
 * </p>
 */
@Configuration
@ConditionalOnVaultiqPersistenceRequirement(VaultiqPersistenceMethod.USE_CACHE)
public class CacheHelperAutoRegistrar {
    public static final Logger log = LoggerFactory.getLogger(CacheHelperAutoRegistrar.class);

    /**
     * Registers a {@link CacheHelper} bean for the {@link CacheType#SESSION_FINGERPRINTS} cache.
     * This bean is named {@link CacheHelper.BeanNames#SESSION_FINGERPRINT_CACHE_HELPER}.
     *
     * @param cacheManager The Spring {@link CacheManager} instance, automatically autowired by Spring.
     * @return A {@link CacheHelper} instance configured to manage the session fingerprints cache.
     */
    @Bean(name = CacheHelper.BeanNames.SESSION_FINGERPRINT_CACHE_HELPER)
    public CacheHelper sessionFingerprintCacheHelper(CacheManager cacheManager) {
        logCacheHelperCreation(CacheType.SESSION_FINGERPRINTS);
        return new CacheHelper(cacheManager, CacheType.SESSION_FINGERPRINTS);
    }

    /**
     * Registers a {@link CacheHelper} bean for the {@link CacheType#SESSION_POOL} cache.
     * This bean is named {@link CacheHelper.BeanNames#SESSION_POOL_CACHE_HELPER}.
     *
     * @param cacheManager The Spring {@link CacheManager} instance, automatically autowired by Spring.
     * @return A {@link CacheHelper} instance configured to manage the session pool cache.
     */
    @Bean(name = CacheHelper.BeanNames.SESSION_POOL_CACHE_HELPER)
    public CacheHelper sessionPoolCacheHelper(CacheManager cacheManager) {
        logCacheHelperCreation(CacheType.SESSION_POOL);
        return new CacheHelper(cacheManager, CacheType.SESSION_POOL);
    }

    /**
     * Registers a {@link CacheHelper} bean for the {@link CacheType#REVOKED_SIDS} cache.
     * This bean is named {@link CacheHelper.BeanNames#REVOKED_SIDS_CACHE_HELPER}.
     *
     * @param cacheManager The Spring {@link CacheManager} instance, automatically autowired by Spring.
     * @return A {@link CacheHelper} instance configured to manage the revoked SIDs cache.
     */
    @Bean(name = CacheHelper.BeanNames.REVOKED_SIDS_CACHE_HELPER)
    public CacheHelper revokedSidsCacheHelper(CacheManager cacheManager) {
        logCacheHelperCreation(CacheType.REVOKED_SIDS);
        return new CacheHelper(cacheManager, CacheType.REVOKED_SIDS);
    }

    /**
     * Registers a {@link CacheHelper} bean for the {@link CacheType#REVOKED_SESSION_POOL} cache.
     * This bean is named {@link CacheHelper.BeanNames#REVOKED_SESSION_POOL_CACHE_HELPER}.
     *
     * @param cacheManager The Spring {@link CacheManager} instance, automatically autowired by Spring.
     * @return A {@link CacheHelper} instance configured to manage the revoked session pool cache.
     */
    @Bean(name = CacheHelper.BeanNames.REVOKED_SESSION_POOL_CACHE_HELPER)
    public CacheHelper revokedSessionPoolCacheHelper(CacheManager cacheManager) {
        logCacheHelperCreation(CacheType.REVOKED_SESSION_POOL);
        return new CacheHelper(cacheManager, CacheType.REVOKED_SESSION_POOL);
    }

    /**
     * Logs the creation of a {@code CacheHelper} bean for a specific cache type.
     *
     * @param cacheType The alias of the cache type for which the bean is being registered.
     */
    private static void logCacheHelperCreation(CacheType cacheType) {
        log.debug("Registering CacheHelper bean for CacheType: {}", cacheType.alias());
    }

}