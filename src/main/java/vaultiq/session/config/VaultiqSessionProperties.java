package vaultiq.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import vaultiq.session.model.ModelType;
import vaultiq.session.context.VaultiqModelConfigEnhancer;
import vaultiq.session.context.VaultiqSessionContext;

import java.util.List;

/**
 * Configuration properties for the Vaultiq Session Tracking Library.
 * <p>
 * These properties allow for the configuration of {@code vaultiq-session}, particularly
 * concerning persistence strategies for different session data models. Properties
 * are bound from application configuration files (e.g., {@code application.properties},
 * {@code application.yml}) using the prefix {@code "vaultiq.session"}.
 * </p>
 * <p>
 * The configured properties are processed internally by the library (e.g.,
 * by {@link VaultiqModelConfigEnhancer}) to determine the effective persistence
 * settings used by components like {@link VaultiqSessionContext}.
 * </p>
 */
@ConfigurationProperties("vaultiq.session")
public class VaultiqSessionProperties {
    /**
     * Configures "production mode" for session persistence.
     * <p>
     * When set to {@code true}, this enables a simplified configuration mode
     * where both Cache and JPA persistence methods are considered active by
     * default for all {@link ModelType}s. This setting acts as a base default
     * and can be overridden by more specific configurations in {@link Persistence}
     * and {@link ModelPersistenceConfig}.
     * </p>
     * <p>
     * Default is {@code false}.
     * </p>
     */
    private boolean productionMode = false;

    /**
     * Holds the detailed persistence configurations, including global defaults
     * and model-specific overrides.
     */
    private Persistence persistence;

    /**
     * Checks if Production Mode is enabled.
     *
     * @return {@code true} if Production Mode is enabled, {@code false} otherwise.
     */
    public boolean isProductionMode() {
        return productionMode;
    }

    public void setProductionMode(boolean productionMode) {
        this.productionMode = productionMode;
    }

    /**
     * Returns the persistence configuration settings.
     * These settings define how session data is stored and managed.
     *
     * @return the {@link Persistence} configuration object.
     */
    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence == null ? new Persistence() : persistence;
    }

    /**
     * Configures persistence settings for the Vaultiq Session Tracking Library.
     * <p>
     * This includes global cache settings (like enabling/disabling JPA and caching by default)
     * and allows for a list of specific configurations per model type via {@link ModelPersistenceConfig}.
     * </p>
     */
    public static class Persistence {
        /**
         * Global default setting to enable or disable JPA-based persistence for all models.
         * <p>
         * This can be overridden by {@link VaultiqSessionProperties#productionMode} or
         * model-specific settings in {@link ModelPersistenceConfig#useJpa}.
         * </p>
         */
        private Boolean useJpa;

        /**
         * Global default setting to enable or disable cache-based persistence for all models.
         * <p>
         * This can be overridden by {@link VaultiqSessionProperties#productionMode} or
         * model-specific settings in {@link ModelPersistenceConfig#useCache}.
         * </p>
         */
        private Boolean useCache;

        /**
         * The name of the Spring {@link org.springframework.cache.CacheManager} bean
         * that the library should use for caching operations.
         * <p>
         * Defaults to "cacheManager".
         * </p>
         */
        private String manager = "cacheManager";

        /**
         * A list of specific persistence configurations for individual model types ({@link ModelType}).
         * These configurations can override the global settings defined in this {@link Persistence} class.
         */
        private List<ModelPersistenceConfig> models;

        /**
         * Checks if JPA persistence is globally enabled by default.
         *
         * @return {@code true} if JPA is globally enabled, {@code false} otherwise.
         */
        public Boolean isUseJpa() {
            return useJpa;
        }

        public void setUseJpa(Boolean useJpa) {
            this.useJpa = useJpa;
        }

        /**
         * Checks if caching is globally enabled by default.
         *
         * @return {@code true} if caching is globally enabled, {@code false} otherwise.
         */
        public Boolean isUseCache() {
            return useCache;
        }

        public void setUseCache(Boolean useCache) {
            this.useCache = useCache;
        }

        /**
         * Gets the configured name of the Spring CacheManager bean.
         *
         * @return the cache manager bean name.
         */
        public String getManager() {
            return manager;
        }

        public void setManager(String manager) {
            this.manager = manager;
        }

        /**
         * Returns the list of specific persistence configurations per model type.
         * These configurations override the global persistence settings.
         *
         * @return a list of {@link ModelPersistenceConfig} instances.
         */
        public List<ModelPersistenceConfig> getModels() {
            return models;
        }

        public void setModels(List<ModelPersistenceConfig> models) {
            this.models = models;
        }
    }

    /**
     * Persistence configuration for a specific {@link ModelType}.
     * <p>
     * Properties defined here override the global settings in {@link Persistence}
     * for the specified {@link #type model type}. If a property (e.g., {@code useJpa}, {@code useCache},
     * {@code syncInterval}) is {@code null} in this configuration, the corresponding
     * global setting from {@link Persistence} or the {@link VaultiqSessionProperties#productionMode}
     * default will apply.
     * </p>
     */
    public static class ModelPersistenceConfig {
        /**
         * The {@link ModelType} to which this specific configuration applies. This field is mandatory
         * for a model-specific configuration to be valid.
         */
        private ModelType type;
        /**
         * Explicitly enable or disable JPA usage for this model type.
         * If {@code null}, the effective setting is determined by {@link Persistence#isUseJpa()}
         * or the {@link VaultiqSessionProperties#isProductionMode()} default.
         */
        private Boolean useJpa;
        /**
         * Explicitly enable or disable Cache usage for this model type.
         * If {@code null}, the effective setting is determined by {@link Persistence#isUseCache()}
         * or the {@link VaultiqSessionProperties#isProductionMode()} default.
         */
        private Boolean useCache;
        /**
         * Specifies the DB cleanup strategy for this model type.
         * <p>
         * If set, this value overrides the global dbCleanup strategy for this specific model type.
         * </p>
         */
        private String dbCleanup;

        /**
         * Gets the {@link ModelType} this configuration applies to.
         *
         * @return the {@link ModelType}.
         */
        public ModelType getType() {
            return type;
        }

        public void setType(ModelType type) {
            this.type = type;
        }

        /**
         * Gets the explicit JPA usage setting for this model type.
         *
         * @return {@link Boolean#TRUE} to enable JPA, {@link Boolean#FALSE} to disable,
         * or {@code null} to use the global/production-mode default.
         */
        public Boolean getUseJpa() {
            return useJpa;
        }

        public void setUseJpa(Boolean useJpa) {
            this.useJpa = useJpa;
        }

        /**
         * Gets the explicit Cache usage setting for this model type.
         *
         * @return {@link Boolean#TRUE} to enable caching, {@link Boolean#FALSE} to disable,
         * or {@code null} to use the global/production-mode default.
         */
        public Boolean getUseCache() {
            return useCache;
        }

        public void setUseCache(Boolean useCache) {
            this.useCache = useCache;
        }

        /**
         * Gets the DB cleanup strategy for this model type.
         *
         * @return the dbCleanup strategy as a String.
         */
        public String getDbCleanup() {
            return dbCleanup;
        }

        public void setDbCleanup(String dbCleanup) {
            this.dbCleanup = dbCleanup;
        }
    }
}