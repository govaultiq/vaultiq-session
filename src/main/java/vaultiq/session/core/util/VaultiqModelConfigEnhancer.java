
package vaultiq.session.core.util;

import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.model.VaultiqModelConfig;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility for building a complete, normalized Vaultiq model configuration map from application/session properties.
 * <p>
 * Ensures that each {@link ModelType} is associated with a resolved {@link VaultiqModelConfig},
 * filling in missing models and using fallbacks as needed (per-model, global, zenMode).
 */
public final class VaultiqModelConfigEnhancer {

    /**
     * Build a normalized configuration map for all model types based on the application/session properties.
     *
     * @param props Vaultiq session properties holding per-model, global, and zenMode config sections
     * @return a complete config map (never null), with explicit config for all ModelType values
     */
    public static Map<ModelType, VaultiqModelConfig> enhance(VaultiqSessionProperties props) {
        var zenMode = props.isZenMode();
        var global = props.getPersistence().getCacheConfig();

        // Ensure ALL ModelType values have a config entry, auto-generating defaults if needed
        var models = enrichWithMissingConfigs(props);

        return models.stream()
                .map(model -> {
                    ModelType type = model.getType();

                    // Use value order: specific > global > zenMode default
                    boolean useJpa = resolve(model.getUseJpa(), global != null ? global.isUseJpa() : null, zenMode);
                    boolean useCache = resolve(model.getUseCache(), global != null ? global.isUseCache() : null, zenMode);

                    // Sync interval: per-model > global
                    Duration syncInterval = Optional.ofNullable(model.getSyncInterval())
                            .orElseGet(() -> props.getPersistence().getCacheConfig().getSyncInterval());

                    String cacheName = model.getCacheName();
                    if (cacheName == null || cacheName.isBlank())
                        cacheName = type.getAlias(); // fallback to canonical alias

                    return new VaultiqModelConfig(type, cacheName, useJpa, useCache, syncInterval);
                })
                .collect(Collectors.toMap(
                        VaultiqModelConfig::modelType,
                        model -> model,
                        (key1, key2) -> key2 // on conflict (duplicate model type), last one wins
                ));
    }

    /**
     * Ensure all ModelType entries are present as configs, producing explicit "defaults"
     * for any type missing from input.
     *
     * @param props Vaultiq session properties
     * @return list of ModelPersistenceConfig, with one entry for each ModelType in the enum
     */
    private static List<VaultiqSessionProperties.ModelPersistenceConfig> enrichWithMissingConfigs(VaultiqSessionProperties props) {
        // Defensive: fallback to an empty list if models missing from config.
        List<VaultiqSessionProperties.ModelPersistenceConfig> models =
                Optional.ofNullable(props.getPersistence().getModels()).orElseGet(ArrayList::new);

        Set<ModelType> existingTypes = models.stream()
                .map(VaultiqSessionProperties.ModelPersistenceConfig::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<VaultiqSessionProperties.ModelPersistenceConfig> enriched = new ArrayList<>(models);

        for (ModelType type : ModelType.values()) {
            if (!existingTypes.contains(type)) {
                // Add explicit empty/default config for missing ModelType
                var defaultConfig = new VaultiqSessionProperties.ModelPersistenceConfig();
                defaultConfig.setType(type);
                enriched.add(defaultConfig);
            }
        }

        return enriched;
    }

    /**
     * Resolve a boolean config using a per-model, then global, then zenMode.
     *
     * @param specific value from model config may be null
     * @param global   value from global config, may be null
     * @param zenMode  final fallback
     * @return resolved result always non-null
     */
    private static boolean resolve(Boolean specific, Boolean global, boolean zenMode) {
        if (specific != null) return specific;
        if (global != null) return global;
        return zenMode;
    }
}
