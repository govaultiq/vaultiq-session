package vaultiq.session.config.model;

import vaultiq.session.cache.util.CacheType;
import vaultiq.session.context.VaultiqModelConfigEnhancer;
import vaultiq.session.context.VaultiqSessionContext;
import vaultiq.session.model.ModelType;

/**
 * Represents the final, resolved configuration for a specific Vaultiq session data model type.
 * <p>
 * This record holds the effective persistence settings (whether JPA and/or Cache are used),
 * for a given {@link CacheType}.
 * The values in this record are the result of the configuration enhancement process
 * performed by {@link VaultiqModelConfigEnhancer}, which consolidates settings from global
 * properties, model-specific overrides, and zen mode.
 * </p>
 * <p>
 * Instances of this record are stored and provided by {@link VaultiqSessionContext}
 * for use by other library components that need to know the persistence strategy
 * for a particular model type.
 * </p>
 *
 * @param type    The {@link CacheType} this configuration applies to.
 * @param useJpa       {@code true} if JPA persistence is enabled for this model type, {@code false} otherwise.
 * @param useCache     {@code true} if cache persistence is enabled for this model type, {@code false} otherwise.
 * @see CacheType
 * @see VaultiqSessionContext
 * @see VaultiqModelConfigEnhancer
 */
public record VaultiqModelConfig(
        ModelType type,
        boolean useJpa,
        boolean useCache
) {
    @Override
    public String toString() {
        return String.format(
                "{ \"ModelType\": \"%s\", \"useJpa\": %b, \"useCache\": %b }",
                type, useJpa, useCache
        );
    }

}
