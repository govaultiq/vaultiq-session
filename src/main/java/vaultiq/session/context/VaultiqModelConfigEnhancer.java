package vaultiq.session.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.config.model.VaultiqModelConfig;
import vaultiq.session.model.ModelType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class responsible for enhancing and normalizing Vaultiq session model configurations.
 * <p>
 * This class processes the raw {@link VaultiqSessionProperties} to produce a complete
 * and resolved {@link Map} of {@link ModelType} to {@link VaultiqModelConfig}.
 * It ensures that every {@link ModelType} defined in the system has an explicit
 * configuration entry, applying fallback logic based on per-model settings,
 * global settings, and the {@code productionMode} property where applicable.
 * </p>
 * <p>
 * The resulting configuration map represents the effective persistence settings
 * for each model type used by the library's components, such as
 * {@link VaultiqSessionContext}.
 * </p>
 */
public final class VaultiqModelConfigEnhancer {
    private static final Logger log = LoggerFactory.getLogger(VaultiqModelConfigEnhancer.class);

    private VaultiqModelConfigEnhancer() { /* utility */ }

    /**
     * Builds a complete and normalized configuration map for all Vaultiq session model types.
     * Consolidates settings from global, individual model, and production mode properties.
     *
     * @param props The {@link VaultiqSessionProperties} holding the configured settings.
     * @return A map where each {@link ModelType} is mapped to its resolved {@link VaultiqModelConfig}.
     */
    public static Map<ModelType, VaultiqModelConfig> enhance(VaultiqSessionProperties props) {
        log.debug("Enhancing session model configs…");
        var prodMode = props.isProductionMode();
        var global = props.getPersistence();

        var modelMap = buildModelConfigMap(global);
        return buildFinalConfigs(modelMap, global, prodMode);
    }

    /**
     * Builds a map from {@link ModelType} to its specific or default persistence configuration.
     *
     * @param global The global persistence properties.
     * @return A map of {@link ModelType} to {@link VaultiqSessionProperties.ModelPersistenceConfig}.
     */
    private static Map<ModelType, VaultiqSessionProperties.ModelPersistenceConfig>
    buildModelConfigMap(VaultiqSessionProperties.Persistence global) {
        // for each ModelType, pick its override or a default blank config
        return Arrays.stream(ModelType.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        mt -> findOrDefaultModelConfig(global, mt)
                ));
    }

    /**
     * Finds the specific {@link VaultiqSessionProperties.ModelPersistenceConfig} for a {@link ModelType},
     * or creates a default if not found.
     *
     * @param global    The global persistence properties.
     * @param modelType The {@link ModelType} to look up.
     * @return The specific or default {@link VaultiqSessionProperties.ModelPersistenceConfig}.
     */
    private static VaultiqSessionProperties.ModelPersistenceConfig
    findOrDefaultModelConfig(VaultiqSessionProperties.Persistence global, ModelType modelType) {
        var configs = Optional.ofNullable(global)
                .map(VaultiqSessionProperties.Persistence::getModels)
                .orElseGet(ArrayList::new);

        return configs.stream()
                .filter(modelConfig -> modelType.equals(modelConfig.getType()))
                .findFirst()
                .orElseGet(() -> {
                    log.debug("No model-config for {}, using defaults", modelType);
                    var def = new VaultiqSessionProperties.ModelPersistenceConfig();
                    def.setType(modelType);
                    return def;
                });
    }

    /**
     * Builds the final map of {@link ModelType} to {@link VaultiqModelConfig}
     * by resolving {@code useJpa} and {@code useCache} flags based on fallback hierarchy.
     *
     * @param modelMap       A map of {@link ModelType} to its base model configuration.
     * @param global         The global persistence properties.
     * @param productionMode The global production mode flag.
     * @return A map containing the fully resolved {@link VaultiqModelConfig} for each {@link ModelType}.
     */
    private static Map<ModelType, VaultiqModelConfig> buildFinalConfigs(
            Map<ModelType, VaultiqSessionProperties.ModelPersistenceConfig> modelMap,
            VaultiqSessionProperties.Persistence global,
            boolean productionMode) {

        return modelMap.entrySet().stream()
                .map(e -> {
                    var type = e.getKey();
                    var modelCfg = e.getValue();

                    var useJpa = resolve(modelCfg.getUseJpa(),
                            global != null ? global.isUseJpa() : null,
                            productionMode);
                    var useCache = resolve(modelCfg.getUseCache(),
                            global != null ? global.isUseCache() : null,
                            productionMode);
                    log.debug("Resolved Model for Type: {}  -> useJpa={}, useCache={}", type, useJpa, useCache);

                    return new VaultiqModelConfig(type, useJpa, useCache);
                })
                .collect(Collectors.toMap(
                        VaultiqModelConfig::type,
                        Function.identity(),
                        (a, b) -> b,
                        () -> new EnumMap<>(ModelType.class)
                ));
    }

    /**
     * Resolves a boolean config value using a specific > global > productionMode fallback.
     *
     * @param specific       Specific configuration boolean (can be null).
     * @param global         Global configuration boolean (can be null).
     * @param productionMode Default boolean if specific and global are null.
     * @return The resolved boolean value.
     */
    private static boolean resolve(Boolean specific, Boolean global, boolean productionMode) {
        // specific > global > productionMode
        return Optional.ofNullable(specific)
                .orElse(Optional.ofNullable(global).orElse(productionMode));
    }
}