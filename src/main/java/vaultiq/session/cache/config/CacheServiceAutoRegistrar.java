package vaultiq.session.cache.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vaultiq.session.cache.service.internal.SessionRevocationCacheService;
import vaultiq.session.cache.service.internal.VaultiqSessionCacheService;
import vaultiq.session.cache.util.CacheType;
import vaultiq.session.cache.util.VaultiqCacheContext;
import vaultiq.session.config.annotation.ConditionalOnCache;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.ConditionalOnVaultiqPersistenceRequirement;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;
import vaultiq.session.context.VaultiqSessionContext;
import vaultiq.session.core.contracts.UserIdentityAware;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.fingerprint.DeviceFingerprintGenerator;

/**
 * Spring configuration class responsible for auto-registering cache-related services
 * based on the application's Vaultiq session persistence configuration.
 * <p>
 * Services are only created if the overall persistence method is {@link VaultiqPersistenceMethod#USE_CACHE}
 * and specific model configurations and required caches are present.
 * </p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnVaultiqPersistenceRequirement(VaultiqPersistenceMethod.USE_CACHE)
public class CacheServiceAutoRegistrar {
    public static final Logger log = LoggerFactory.getLogger(CacheServiceAutoRegistrar.class);

    /**
     * Registers the {@link VaultiqSessionCacheService} bean.
     * This service is active only if the {@link ModelType#SESSION} is configured to
     * {@link VaultiqPersistenceMethod#USE_CACHE} and the cache for type {@link CacheType#SESSION_POOL} exists.
     *
     * @param context The {@link VaultiqSessionContext} bean.
     * @param cacheContext The {@link VaultiqCacheContext} bean.
     * @param fingerprintGenerator The {@link DeviceFingerprintGenerator} bean.
     * @return A new instance of {@link VaultiqSessionCacheService}.
     */
    @Bean
    @ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.SESSION)
    @ConditionalOnCache(CacheType.SESSION_POOL)
    public VaultiqSessionCacheService vaultiqSessionCacheService(
            VaultiqSessionContext context,
            VaultiqCacheContext cacheContext,
            DeviceFingerprintGenerator fingerprintGenerator) {
        return new VaultiqSessionCacheService(context, cacheContext, fingerprintGenerator);
    }

    /**
     * Registers the {@link SessionRevocationCacheService} bean.
     * This service is active only if the {@link ModelType#REVOKE} is configured to
     * {@link VaultiqPersistenceMethod#USE_CACHE} and the cache for type {@link CacheType#REVOKED_SESSION_POOL} exists.
     *
     * @param cacheContext The {@link VaultiqCacheContext} bean.
     * @param vaultiqSessionCacheService The {@link VaultiqSessionCacheService} bean.
     * @param userIdentityAware The {@link UserIdentityAware} bean.
     * @return A new instance of {@link SessionRevocationCacheService}.
     */
    @Bean
    @ConditionalOnVaultiqModelConfig(method = VaultiqPersistenceMethod.USE_CACHE, type = ModelType.REVOKE)
    @ConditionalOnCache(CacheType.REVOKED_SESSION_POOL)
    public SessionRevocationCacheService sessionRevocationCacheService(
            VaultiqCacheContext cacheContext,
            VaultiqSessionCacheService vaultiqSessionCacheService,
            UserIdentityAware userIdentityAware
    ) {
        return new SessionRevocationCacheService(cacheContext, vaultiqSessionCacheService, userIdentityAware);
    }

}