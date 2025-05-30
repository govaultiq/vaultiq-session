package vaultiq.session.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vaultiq.session.cache.util.CacheType;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.config.model.VaultiqModelConfig;

import java.util.Arrays;
import java.util.Map;

/**
 * Provides access to the resolved and normalized Vaultiq session and model configuration.
 * <p>
 * This component is populated at application startup by processing the
 * {@link VaultiqSessionProperties} through the {@link VaultiqModelConfigEnhancer}.
 * It offers a type-safe and accessible way for other library components to query
 * the effective persistence settings for each {@link ModelType}, the configured
 * cache manager name, and whether caching or JPA persistence is enabled for
 * any configured model type.
 * </p>
 */
public class VaultiqSessionContext {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionContext.class);
    private final String cacheManagerName;
    private final Map<CacheType, VaultiqModelConfig> modelConfigs;
    private final boolean isUsingCache;
    private final boolean isUsingJpa;

    /**
     * Constructs the session context by processing the provided Vaultiq properties.
     * <p>
     * This constructor uses {@link VaultiqModelConfigEnhancer} to build the
     * comprehensive configuration map for all model types and determines the
     * aggregate cache and JPA usage status across all configured models.
     * </p>
     *
     * @param props the Vaultiq session/application properties, typically injected by Spring.
     */
    public VaultiqSessionContext(VaultiqSessionProperties props) {
        log.debug("Initializing VaultiqSessionContext with properties.");
        this.cacheManagerName = props.getPersistence().getManager();
        this.modelConfigs = VaultiqModelConfigEnhancer.enhance(props);
        this.isUsingCache = checkIfUsingCache();
        this.isUsingJpa = checkIfUsingJpa();
        logContext();
    }

    private void logContext() {
        if(isUsingCache)
            log.debug("Vaultiq Session Context initialized; is-using-jpa: {}. is-using-cache: {}, cache-manager-name: {}", isUsingJpa, true, cacheManagerName);
        else
            log.debug("Vaultiq Session Context initialized; is-using-jpa: {}. is-using-cache: {}", isUsingJpa, false);

        var configs = Arrays.stream(CacheType.values())
                .map(this::getModelConfig)
                .map(VaultiqModelConfig::toString)
                .toList();

        log.debug("Resolved Model configurations After Enrichment: {}", configs);
    }

    /**
     * Checks if cache-based persistence is enabled for *any* configured model type.
     *
     * @return {@code true} if at least one model type is configured to use cache, {@code false} otherwise.
     */
    private boolean checkIfUsingCache() {
        return modelConfigs.values().stream().anyMatch(VaultiqModelConfig::useCache);
    }

    /**
     * Checks if JPA-based persistence is enabled for *any* configured model type.
     *
     * @return {@code true} if at least one model type is configured to use JPA, {@code false} otherwise.
     */
    private boolean checkIfUsingJpa() {
        return modelConfigs.values().stream().anyMatch(VaultiqModelConfig::useJpa);
    }

    /**
     * Returns the name of the Spring {@link org.springframework.cache.CacheManager}
     * configured for use by the Vaultiq session library.
     * <p>
     * This name is derived from the {@code vaultiq.session.persistence.cache-config.manager}
     * property.
     * </p>
     *
     * @return the configured cache manager name.
     */
    public String getCacheManagerName() {
        return cacheManagerName;
    }

    /**
     * Retrieves the resolved and enhanced configuration for a specific session data model type.
     * <p>
     * This configuration includes the effective persistence methods (cache/JPA),
     * cache name, and sync interval for the given model type, after applying
     * overrides from {@link VaultiqSessionProperties}.
     * </p>
     *
     * @param type the {@link CacheType} of the session/data model.
     * @return the resolved {@link VaultiqModelConfig} for the specified type. This is
     * never {@code null} for a valid {@link CacheType} as {@link VaultiqModelConfigEnhancer}
     * ensures all types have a configuration entry.
     */
    public VaultiqModelConfig getModelConfig(CacheType type) {
        return modelConfigs.get(type);
    }

    /**
     * Indicates whether cache-based persistence is active for any model type
     * based on the resolved configuration.
     *
     * @return {@code true} if caching is used for at least one model type, {@code false} otherwise.
     */
    public boolean isUsingCache() {
        return isUsingCache;
    }

    /**
     * Indicates whether JPA-based persistence is active for any model type
     * based on the resolved configuration.
     *
     * @return {@code true} if JPA is used for at least one model type, {@code false} otherwise.
     */
    public boolean isUsingJpa() {
        return isUsingJpa;
    }
}
