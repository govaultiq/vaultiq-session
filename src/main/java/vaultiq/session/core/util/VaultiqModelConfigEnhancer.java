package vaultiq.session.core.util;

import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.model.VaultiqModelConfig;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class responsible for enhancing and normalizing Vaultiq session model configurations.
 * <p>
 * This class processes the raw {@link VaultiqSessionProperties} to produce a complete
 * and resolved {@link Map} of {@link ModelType} to {@link VaultiqModelConfig}.
 * It ensures that every {@link ModelType} defined in the system has an explicit
 * configuration entry, applying fallback logic based on per-model settings,
 * global settings, and the {@code zenMode} property where applicable.
 * </p>
 * <p>
 * The resulting configuration map represents the effective persistence settings
 * for each model type used by the library's components, such as
 * {@link VaultiqSessionContext}.
 * </p>
 */
public final class VaultiqModelConfigEnhancer {

    // Private constructor to prevent instantiation of this utility class.
    private VaultiqModelConfigEnhancer() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Builds a complete and normalized configuration map for all Vaultiq session
     * model types based on the provided application properties.
     * <p>
     * This method consolidates settings from global cache configuration,
     * individual model configurations, and the {@code zenMode} property
     * according to a defined fallback hierarchy to determine the effective
     * persistence settings for each {@link ModelType}.
     * </p>
     *
     * @param props The {@link VaultiqSessionProperties} holding the configured settings.
     * @return A complete {@link Map} where each {@link ModelType} is mapped to its
     * resolved {@link VaultiqModelConfig}. This map is never {@code null} and
     * contains an entry for every value in the {@link ModelType} enum.
     */
    public static Map<ModelType, VaultiqModelConfig> enhance(VaultiqSessionProperties props) {
        var zenMode = props.isZenMode();
        var global = props.getPersistence().getCacheConfig();

        // Ensure ALL ModelType values have a config entry, auto-generating defaults if needed
        var models = enrichWithMissingConfigs(props);

        return models.stream()
                .map(model -> {
                    ModelType type = model.getType();

                    // Resolve boolean persistence flags (useJpa, useCache) using the fallback hierarchy:
                    // 1. Specific model config (if present)
                    // 2. Global cache config (if present)
                    // 3. Zen mode (final fallback)
                    boolean useJpa = resolve(model.getUseJpa(), global != null ? global.isUseJpa() : null, zenMode);
                    boolean useCache = resolve(model.getUseCache(), global != null ? global.isUseCache() : null, zenMode);

                    // Resolve sync interval: per-model override > global setting.
                    Duration syncInterval = Optional.ofNullable(model.getSyncInterval())
                            .orElseGet(() -> props.getPersistence().getCacheConfig().getSyncInterval());

                    // Resolve cache name: per-model override > ModelType alias.
                    String cacheName = model.getCacheName();
                    if (cacheName == null || cacheName.isBlank())
                        cacheName = type.getAlias(); // fallback to canonical alias

                    // Create the final, resolved VaultiqModelConfig for this model type.
                    return new VaultiqModelConfig(type, cacheName, useJpa, useCache, syncInterval);
                })
                .collect(Collectors.toMap(
                        VaultiqModelConfig::modelType, // Key mapper: ModelType
                        model -> model, // Value mapper: VaultiqModelConfig
                        (key1, key2) -> key2 // Merge function: if duplicate ModelTypes, the last one processed wins (should not happen with enrichWithMissingConfigs)
                ));
    }

    /**
     * Ensures that the list of {@link VaultiqSessionProperties.ModelPersistenceConfig}
     * contains an entry for every {@link ModelType} defined in the enum.
     * <p>
     * If a {@link ModelType} is missing from the input list, a default
     * {@link VaultiqSessionProperties.ModelPersistenceConfig} with only the
     * {@link ModelType} set is added. This guarantees that the {@link #enhance(VaultiqSessionProperties)}
     * method has a starting configuration object for every model type before applying fallbacks.
     * </p>
     *
     * @param props The {@link VaultiqSessionProperties}.
     * @return A {@link List} of {@link VaultiqSessionProperties.ModelPersistenceConfig}
     * containing exactly one entry for each {@link ModelType} value.
     */
    private static List<VaultiqSessionProperties.ModelPersistenceConfig> enrichWithMissingConfigs(VaultiqSessionProperties props) {
        // Defensive: fallback to an empty list if models list is missing from config.
        List<VaultiqSessionProperties.ModelPersistenceConfig> models =
                Optional.ofNullable(props.getPersistence().getModels()).orElseGet(ArrayList::new);

        // Collect all ModelTypes that already have a configuration entry.
        Set<ModelType> existingTypes = models.stream()
                .map(VaultiqSessionProperties.ModelPersistenceConfig::getType)
                .filter(Objects::nonNull) // Filter out entries with null type, though ideally config validation prevents this.
                .collect(Collectors.toSet());

        // Create a new list starting with the existing configurations.
        List<VaultiqSessionProperties.ModelPersistenceConfig> enriched = new ArrayList<>(models);

        // Iterate through all possible ModelType values.
        for (ModelType type : ModelType.values()) {
            // If a ModelType does not have an existing configuration, add a default one.
            if (!existingTypes.contains(type)) {
                var defaultConfig = new VaultiqSessionProperties.ModelPersistenceConfig();
                defaultConfig.setType(type); // Set the type for the missing configuration.
                enriched.add(defaultConfig);
            }
        }

        return enriched;
    }

    /**
     * Resolves a boolean configuration value (e.g., {@code useCache}, {@code useJpa})
     * by applying a specific fallback hierarchy: specific model config > global config > zen mode.
     *
     * @param specific The boolean value from the specific {@link ModelPersistenceConfig} (may be {@code null}).
     * @param global   The boolean value from the global {@link CacheConfig} (may be {@code null}).
     * @param zenMode  The value of the {@code zenMode} property (the final fallback).
     * @return The resolved boolean value, which is guaranteed to be non-null.
     */
    private static boolean resolve(Boolean specific, Boolean global, boolean zenMode) {
        // If a specific value is provided, use it.
        if (specific != null) return specific;
        // If no specific value, but a global value is provided, use it.
        if (global != null) return global;
        // If neither specific nor global is provided, use the zen mode value as the final fallback.
        return zenMode;
    }
}
