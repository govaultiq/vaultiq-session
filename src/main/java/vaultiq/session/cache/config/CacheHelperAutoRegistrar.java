package vaultiq.session.cache.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vaultiq.session.cache.util.CacheHelper;
import vaultiq.session.cache.util.CacheType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistenceRequirement;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.model.ModelType;

import java.util.Optional;

/**
 * <p>
 * Spring {@code @Configuration} class responsible for automatically registering
 * {@link CacheHelper} beans into the application context.
 * </p>
 *
 * <p>
 * This configuration class itself is active only when the application's global
 * persistence requirement is set to {@link VaultiqPersistenceMethod#USE_CACHE},
 * as determined by {@code @ConditionalOnVaultiqPersistenceRequirement}.
 * </p>
 *
 * <p>
 * Each {@code CacheHelper} bean is specifically configured for a particular {@link CacheType}
 * and is named using constants from {@link CacheHelper.BeanNames} for consistent
 * and compile-time safe referencing. Additionally, individual {@code CacheHelper}
 * beans are conditionally registered based on specific {@link ModelType} configurations
 * via {@code @ConditionalOnVaultiqModelConfig}.
 * </p>
 *
 * <p>
 * This registrar is designed to handle the absence of a CacheManager gracefully.
 * If no CacheManager bean is available in the application context, it will log a warning
 * and continue to register CacheHelper beans that will silently skip all cache operations.
 * This approach allows the application to function without caching when a CacheManager
 * is not configured, without throwing exceptions or disrupting application flow.
 * </p>
 */
@Configuration
@ConditionalOnVaultiqPersistenceRequirement(VaultiqPersistenceMethod.USE_CACHE)
public class CacheHelperAutoRegistrar {
    public static final Logger log = LoggerFactory.getLogger(CacheHelperAutoRegistrar.class);

    private final CacheManager cacheManager;

    public CacheHelperAutoRegistrar(Optional<CacheManager> optionalCacheManager) {
        this.cacheManager = optionalCacheManager.orElseGet(() -> {
            log.warn("CacheManager not available. Required for Caching. Consider defining a bean of CacheManager.");
            return null;
        });
        log.debug("CacheHelperAutoRegistrar initialized.");
    }

    /**
     * Provides an {@link Optional} wrapper around the CacheManager instance.
     * This method is a key part of the silent failure mechanism, ensuring that
     * CacheHelper instances can be created even when the CacheManager is not available.
     * 
     * @return An {@link Optional} containing the CacheManager if it was successfully initialized,
     *         otherwise an empty Optional if no CacheManager was available at startup.
     */
    private Optional<CacheManager> getCacheManger() {
        return Optional.ofNullable(cacheManager);
    }

    /**
     * Registers a {@link CacheHelper} bean for the {@link CacheType#SESSION_FINGERPRINTS} cache.
     * This bean is named {@link CacheHelper.BeanNames#SESSION_FINGERPRINT_CACHE_HELPER}.
     * <p>
     * This bean's creation is further conditioned by {@code @ConditionalOnVaultiqModelConfig},
     * requiring {@link VaultiqPersistenceMethod#USE_CACHE} for {@link ModelType#SESSION} data.
     * </p>
     *
     * @return A {@link CacheHelper} instance configured to manage the session fingerprints cache.
     */
    @Bean(name = CacheHelper.BeanNames.SESSION_FINGERPRINT_CACHE_HELPER)
    @ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.SESSION)
    public CacheHelper sessionFingerprintCacheHelper() {
        logCacheHelperCreation(CacheType.SESSION_FINGERPRINTS);
        return new CacheHelper(getCacheManger(), CacheType.SESSION_FINGERPRINTS);
    }

    /**
     * Registers a {@link CacheHelper} bean for the {@link CacheType#SESSION_POOL} cache.
     * This bean is named {@link CacheHelper.BeanNames#SESSION_POOL_CACHE_HELPER}.
     * <p>
     * This bean's creation is further conditioned by {@code @ConditionalOnVaultiqModelConfig},
     * requiring {@link VaultiqPersistenceMethod#USE_CACHE} for {@link ModelType#SESSION} data.
     * </p>
     *
     * @return A {@link CacheHelper} instance configured to manage the session pool cache.
     */
    @Bean(name = CacheHelper.BeanNames.SESSION_POOL_CACHE_HELPER)
    @ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.SESSION)
    public CacheHelper sessionPoolCacheHelper() {
        logCacheHelperCreation(CacheType.SESSION_POOL);
        return new CacheHelper(getCacheManger(), CacheType.SESSION_POOL);
    }

    /**
     * Registers a {@link CacheHelper} bean for the {@link CacheType#REVOKED_SIDS} cache.
     * This bean is named {@link CacheHelper.BeanNames#REVOKED_SIDS_CACHE_HELPER}.
     * <p>
     * This bean's creation is further conditioned by {@code @ConditionalOnVaultiqModelConfig},
     * requiring {@link VaultiqPersistenceMethod#USE_CACHE} for {@link ModelType#REVOKE} data.
     * </p>
     *
     * @return A {@link CacheHelper} instance configured to manage the revoked SIDs cache.
     */
    @Bean(name = CacheHelper.BeanNames.REVOKED_SIDS_CACHE_HELPER)
    @ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.REVOKE)
    public CacheHelper revokedSidsCacheHelper() {
        logCacheHelperCreation(CacheType.REVOKED_SIDS);
        return new CacheHelper(getCacheManger(), CacheType.REVOKED_SIDS);
    }

    /**
     * Registers a {@link CacheHelper} bean for the {@link CacheType#REVOKED_SESSION_POOL} cache.
     * This bean is named {@link CacheHelper.BeanNames#REVOKED_SESSION_POOL_CACHE_HELPER}.
     * <p>
     * This bean's creation is further conditioned by {@code @ConditionalOnVaultiqModelConfig},
     * requiring {@link VaultiqPersistenceMethod#USE_CACHE} for {@link ModelType#REVOKE} data.
     * </p>
     *
     * @return A {@link CacheHelper} instance configured to manage the revoked session pool cache.
     */
    @Bean(name = CacheHelper.BeanNames.REVOKED_SESSION_POOL_CACHE_HELPER)
    @ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.REVOKE)
    public CacheHelper revokedSessionPoolCacheHelper() {
        logCacheHelperCreation(CacheType.REVOKED_SESSION_POOL);
        return new CacheHelper(getCacheManger(), CacheType.REVOKED_SESSION_POOL);
    }

    /**
     * Logs the registration of a {@code CacheHelper} bean for a specific {@link CacheType}.
     * This method uses the {@code DEBUG} log level to avoid cluttering INFO level output
     * during application startup.
     *
     * @param cacheType The {@link CacheType} for which the bean is being registered.
     */
    private static void logCacheHelperCreation(CacheType cacheType) {
        log.debug("Registering CacheHelper bean for CacheType: {}", cacheType.alias());
    }
}