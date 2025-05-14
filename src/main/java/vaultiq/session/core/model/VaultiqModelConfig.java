package vaultiq.session.core.model;

import vaultiq.session.cache.model.ModelType;

import java.time.Duration;

/**
 * Represents the final, resolved configuration for a specific Vaultiq session data model type.
 * <p>
 * This record holds the effective persistence settings (whether JPA and/or Cache are used),
 * the specific cache name, and the synchronization interval for a given {@link ModelType}.
 * The values in this record are the result of the configuration enhancement process
 * performed by {@link vaultiq.session.core.util.VaultiqModelConfigEnhancer}, which
 * consolidates settings from global properties, model-specific overrides, and zen mode.
 * </p>
 * <p>
 * Instances of this record are stored and provided by {@link vaultiq.session.core.util.VaultiqSessionContext}
 * for use by other library components that need to know the persistence strategy
 * for a particular model type.
 * </p>
 *
 * @param modelType    The {@link ModelType} this configuration applies to.
 * @param cacheName    The effective name of the cache to be used for this model type.
 * @param useJpa       {@code true} if JPA persistence is enabled for this model type, {@code false} otherwise.
 * @param useCache     {@code true} if cache persistence is enabled for this model type, {@code false} otherwise.
 * @param syncInterval The effective synchronization interval for this model type, if applicable.
 * @see ModelType
 * @see vaultiq.session.core.util.VaultiqSessionContext
 * @see vaultiq.session.core.util.VaultiqModelConfigEnhancer
 */
public record VaultiqModelConfig(
        ModelType modelType,
        String cacheName,
        boolean useJpa,
        boolean useCache,
        Duration syncInterval
) {
}
