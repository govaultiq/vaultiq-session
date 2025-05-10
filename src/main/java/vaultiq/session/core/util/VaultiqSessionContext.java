
package vaultiq.session.core.util;

import org.springframework.stereotype.Component;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.model.VaultiqModelConfig;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Exposes resolved Vaultiq session/model configuration in a type-safe, accessible form for the application.
 * <p>
 * Populates at startup using {@link VaultiqModelConfigEnhancer}.
 * Provides model-type config lookup,
 * effective cache manager name, and whether caching or JPA is enabled for any configured model type.
 */
@Component
public class VaultiqSessionContext {

    private final String cacheManagerName;
    private final Map<ModelType, VaultiqModelConfig> modelConfigs;
    private final boolean isUsingCache;
    private final boolean isUsingJpa;

    /**
     * Constructs the session context using provided Vaultiq properties.
     * Builds a config map and determines aggregate cache/JPA settings for the application.
     *
     * @param props the Vaultiq session/application properties (should be Spring-injected)
     */
    public VaultiqSessionContext(VaultiqSessionProperties props) {
        this.cacheManagerName = props.getPersistence().getCacheConfig().getManager();
        this.modelConfigs = VaultiqModelConfigEnhancer.enhance(props);
        this.isUsingCache = checkIfUsingCache();
        this.isUsingJpa = checkIfUsingJpa();
    }

    /**
     * Returns true iff any configured model type is using cache-based persistence.
     */
    private boolean checkIfUsingCache() {
        return modelConfigs.values().stream().anyMatch(VaultiqModelConfig::useCache);
    }

    /**
     * Returns true iff any configured model type is using JPA-based persistence.
     */
    private boolean checkIfUsingJpa() {
        return modelConfigs.values().stream().anyMatch(VaultiqModelConfig::useJpa);
    }

    /**
     * @return the cache manager name configured for all models in this context.
     */
    public String getCacheManagerName() {
        return cacheManagerName;
    }

    /**
     * Lookup the configuration for a single model type.
     * @param type type of session/data model
     * @return the resolved config (never null for a known type)
     */
    public VaultiqModelConfig getModelConfig(ModelType type) {
        return modelConfigs.get(type);
    }

    /**
     * @return true if any model type uses cache-based persistence
     */
    public boolean isUsingCache() {
        return isUsingCache;
    }

    /**
     * @return true if any model type uses JPA-based persistence
     */
    public boolean isUsingJpa() {
        return isUsingJpa;
    }
}
