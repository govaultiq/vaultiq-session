package vaultiq.session.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(VaultiqModelConfigEnhancer.class);

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
        log.debug("Enhancing Vaultiq session model configurations with properties: {}", props);
        var zenMode = props.isZenMode();
        var global = props.getPersistence().getCacheConfig();

        var models = enrichWithMissingConfigs(props);
        log.debug("Total models after enrichment: {}", models.size());

        return models.stream()
                .map(model -> {
                    ModelType type = model.getType();
                    log.debug("Processing model type: {}", type);

                    boolean useJpa = resolve(model.getUseJpa(), global != null ? global.isUseJpa() : null, zenMode);
                    boolean useCache = resolve(model.getUseCache(), global != null ? global.isUseCache() : null, zenMode);
                    log.debug("Resolved useJpa: {}, useCache: {} for model type: {}", useJpa, useCache, type);

                    Duration syncInterval = Optional.ofNullable(model.getSyncInterval())
                            .orElseGet(() -> props.getPersistence().getCacheConfig().getSyncInterval());
                    log.debug("Resolved sync interval for model type {}: {}", type, syncInterval);

                    String cacheName = model.getCacheName();
                    if (cacheName == null || cacheName.isBlank()) {
                        cacheName = type.getAlias();
                        log.debug("Cache name for model type {} was blank. Using alias: {}", type, cacheName);
                    }

                    var resolvedConfig = new VaultiqModelConfig(type, cacheName, useJpa, useCache, syncInterval);
                    log.debug("Final resolved config for model type {}: {}", type, resolvedConfig);
                    return resolvedConfig;
                })
                .collect(Collectors.toMap(
                        VaultiqModelConfig::modelType,
                        model -> model,
                        (key1, key2) -> key2
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
        List<VaultiqSessionProperties.ModelPersistenceConfig> models =
                Optional.ofNullable(props.getPersistence().getModels()).orElseGet(ArrayList::new);

        Set<ModelType> existingTypes = models.stream()
                .map(VaultiqSessionProperties.ModelPersistenceConfig::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<VaultiqSessionProperties.ModelPersistenceConfig> enriched = new ArrayList<>(models);

        for (ModelType type : ModelType.values()) {
            if (!existingTypes.contains(type)) {
                var defaultConfig = new VaultiqSessionProperties.ModelPersistenceConfig();
                defaultConfig.setType(type);
                enriched.add(defaultConfig);
                log.debug("Added default config for missing model type: {}", type);
            }
        }

        log.debug("Enriched model configs size: {}", enriched.size());
        return enriched;
    }

    /**
     * Resolves a boolean configuration value (e.g., {@code useCache}, {@code useJpa})
     * by applying a specific fallback hierarchy: specific model config > global config > zen mode.
     *
     * @param specific The boolean value from the specific {@link vaultiq.session.config.VaultiqSessionProperties.ModelPersistenceConfig} (maybe {@code null}).
     * @param global   The boolean value from the global {@link vaultiq.session.config.VaultiqSessionProperties.CacheConfig} (maybe {@code null}).
     * @param zenMode  The value of the {@code zenMode} property (the final fallback).
     * @return The resolved boolean value, which is guaranteed to be non-null.
     */
    private static boolean resolve(Boolean specific, Boolean global, boolean zenMode) {
        if (specific != null) return specific;
        if (global != null) return global;
        return zenMode;
    }
}