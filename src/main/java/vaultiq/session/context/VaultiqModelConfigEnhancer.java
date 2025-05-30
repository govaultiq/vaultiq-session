package vaultiq.session.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vaultiq.session.cache.util.CacheType;
import vaultiq.session.core.model.ModelType;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.config.model.VaultiqModelConfig;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class responsible for enhancing and normalizing Vaultiq session model configurations.
 * <p>
 * This class processes the raw {@link VaultiqSessionProperties} to produce a complete
 * and resolved {@link Map} of {@link CacheType} to {@link VaultiqModelConfig}.
 * It ensures that every {@link CacheType} defined in the system has an explicit
 * configuration entry, applying fallback logic based on per-model settings,
 * global settings, and the {@code zenMode} property where applicable.
 * </p>
 * <p>
 * The resulting configuration map represents the effective persistence settings
 * for each cache type used by the library's components, such as
 * {@link VaultiqSessionContext}.
 * </p>
 */
public final class VaultiqModelConfigEnhancer {
    private static final Logger log = LoggerFactory.getLogger(VaultiqModelConfigEnhancer.class);

    private VaultiqModelConfigEnhancer() { /* utility */ }

    /**
     * Builds a complete and normalized configuration map for all Vaultiq session cache types.
     * Consolidates settings from global, individual model, and zen mode properties.
     *
     * @param props The {@link VaultiqSessionProperties} holding the configured settings.
     * @return A map where each {@link CacheType} is mapped to its resolved {@link VaultiqModelConfig}.
     */
    public static Map<CacheType, VaultiqModelConfig> enhance(VaultiqSessionProperties props) {
        log.debug("Enhancing session model configs…");
        var zenMode = props.isZenMode();
        var global = props.getPersistence();

        var modelMap = buildModelConfigMap(global);
        var perCache = buildPerCacheMap(modelMap);
        return buildFinalConfigs(perCache, global, zenMode);
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
     * Translates {@link ModelType}-based configurations into {@link CacheType}-based configurations.
     *
     * @param modelMap A map from {@link ModelType} to its base configuration.
     * @return An {@link EnumMap} mapping {@link CacheType} to its corresponding model configuration.
     */
    private static Map<CacheType, VaultiqSessionProperties.ModelPersistenceConfig>
    buildPerCacheMap(Map<ModelType, VaultiqSessionProperties.ModelPersistenceConfig> modelMap) {
        var perCache = new EnumMap<CacheType, VaultiqSessionProperties.ModelPersistenceConfig>(CacheType.class);
        modelMap.values()
                .forEach(mc -> CacheType.getByModelType(mc.getType())
                        .forEach(ct -> perCache.put(ct, mc)));
        return perCache;
    }

    /**
     * Builds the final map of {@link CacheType} to {@link VaultiqModelConfig}
     * by resolving {@code useJpa} and {@code useCache} flags based on fallback hierarchy.
     *
     * @param perCache A map of {@link CacheType} to its base model configuration.
     * @param global   The global persistence properties.
     * @param zenMode  The global zen mode flag.
     * @return A map containing the fully resolved {@link VaultiqModelConfig} for each {@link CacheType}.
     */
    private static Map<CacheType, VaultiqModelConfig> buildFinalConfigs(
            Map<CacheType, VaultiqSessionProperties.ModelPersistenceConfig> perCache,
            VaultiqSessionProperties.Persistence global,
            boolean zenMode) {

        return perCache.entrySet().stream()
                .map(e -> {
                    var type = e.getKey();
                    var modelCfg = e.getValue();

                    var useJpa = resolve(modelCfg.getUseJpa(),
                            global != null ? global.isUseJpa() : null,
                            zenMode);
                    var useCache = resolve(modelCfg.getUseCache(),
                            global != null ? global.isUseCache() : null,
                            zenMode);
                    log.debug("Resolved Cache for Type: {}  -> useJpa={}, useCache={}", type, useJpa, useCache);

                    return new VaultiqModelConfig(type, type.alias(), useJpa, useCache);
                })
                .collect(Collectors.toMap(
                        VaultiqModelConfig::type,
                        Function.identity(),
                        (a, b) -> b,
                        () -> new EnumMap<>(CacheType.class)
                ));
    }

    /**
     * Resolves a boolean config value using a specific > global > zenMode fallback.
     *
     * @param specific Specific configuration boolean (can be null).
     * @param global   Global configuration boolean (can be null).
     * @param zenMode  Default boolean if specific and global are null.
     * @return The resolved boolean value.
     */
    private static boolean resolve(Boolean specific, Boolean global, boolean zenMode) {
        // specific > global > zenMode
        return Optional.ofNullable(specific)
                .orElse(Optional.ofNullable(global).orElse(zenMode));
    }
}