package vaultiq.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import vaultiq.session.cache.util.CacheType;
import vaultiq.session.context.VaultiqModelConfigEnhancer;
import vaultiq.session.context.VaultiqSessionContext;

import java.time.Duration;
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
     * Configures "zen mode" for session persistence.
     * <p>
     * When set to {@code true}, this enables a simplified configuration mode
     * where both Cache and JPA persistence methods are considered active by
     * default for all {@link CacheType}s. This setting acts as a base default
     * and can be overridden by more specific configurations in {@link Persistence}
     * and {@link ModelPersistenceConfig}.
     * </p>
     * <p>
     * Default is {@code false}.
     * </p>
     */
    private boolean zenMode = false;

    /**
     * Holds the detailed persistence configurations, including global defaults
     * and model-specific overrides.
     */
    private Persistence persistence;

    /**
     * Checks if Zen Mode is enabled.
     *
     * @return {@code true} if Zen Mode is enabled, {@code false} otherwise.
     */
    public boolean isZenMode() {
        return zenMode;
    }

    public void setZenMode(boolean zenMode) {
        this.zenMode = zenMode;
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
         * This can be overridden by {@link VaultiqSessionProperties#zenMode} or
         * model-specific settings in {@link ModelPersistenceConfig#useJpa}.
         * Defaults to {@code false}.
         * </p>
         */
        private boolean useJpa = false;

        /**
         * Global default setting to enable or disable cache-based persistence for all models.
         * <p>
         * This can be overridden by {@link VaultiqSessionProperties#zenMode} or
         * model-specific settings in {@link ModelPersistenceConfig#useCache}.
         * Defaults to {@code false}.
         * </p>
         */
        private boolean useCache = false;

        /**
         * The name of the Spring {@link org.springframework.cache.CacheManager} bean
         * that the library should use for caching operations.
         * <p>
         * Defaults to "cacheManager".
         * </p>
         */
        private String manager = "cacheManager";

        /**
         * The default interval for cache synchronization operations, if supported and
         * applicable to the configured cache implementation. This can be overridden
         * by model-specific settings in {@link ModelPersistenceConfig#syncInterval}.
         * <p>
         * Defaults to 5 minutes.
         * </p>
         */
        private Duration syncInterval = Duration.ofMinutes(5);

        /**
         * A list of specific persistence configurations for individual model types ({@link CacheType}).
         * These configurations can override the global settings defined in this {@link Persistence} class.
         */
        private List<ModelPersistenceConfig> models;

        /**
         * Checks if JPA persistence is globally enabled by default.
         *
         * @return {@code true} if JPA is globally enabled, {@code false} otherwise.
         */
        public boolean isUseJpa() {
            return useJpa;
        }

        public void setUseJpa(boolean useJpa) {
            this.useJpa = useJpa;
        }

        /**
         * Checks if caching is globally enabled by default.
         *
         * @return {@code true} if caching is globally enabled, {@code false} otherwise.
         */
        public boolean isUseCache() {
            return useCache;
        }

        public void setUseCache(boolean useCache) {
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
         * Gets the global default cache synchronization interval.
         *
         * @return the global sync interval.
         */
        public Duration getSyncInterval() {
            return syncInterval;
        }

        public void setSyncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
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
     * Persistence configuration for a specific {@link CacheType}.
     * <p>
     * Properties defined here override the global settings in {@link Persistence}
     * for the specified {@link #type model type}. If a property (e.g., {@code useJpa}, {@code useCache},
     * {@code syncInterval}) is {@code null} in this configuration, the corresponding
     * global setting from {@link Persistence} or the {@link VaultiqSessionProperties#zenMode}
     * default will apply.
     * </p>
     */
    public static class ModelPersistenceConfig {
        /**
         * The {@link CacheType} to which this specific configuration applies. This field is mandatory
         * for a model-specific configuration to be valid.
         */
        private CacheType type;
        /**
         * The specific cache name to use for this model type within the configured
         * {@link Persistence#manager cache manager}.
         * If not specified, the library might fall back to a default name
         * derived from the {@link CacheType}'s alias (e.g., {@code CacheType#getAlias()}).
         */
        private String cacheName;
        /**
         * Explicitly enable or disable JPA usage for this model type.
         * If {@code null}, the effective setting is determined by {@link Persistence#isUseJpa()}
         * or the {@link VaultiqSessionProperties#isZenMode()} default.
         */
        private Boolean useJpa;
        /**
         * Explicitly enable or disable Cache usage for this model type.
         * If {@code null}, the effective setting is determined by {@link Persistence#isUseCache()}
         * or the {@link VaultiqSessionProperties#isZenMode()} default.
         */
        private Boolean useCache;
        /**
         * The synchronization interval specifically for this model type.
         * If {@code null}, the global {@link Persistence#getSyncInterval()} is used.
         */
        private Duration syncInterval;

        /**
         * Gets the {@link CacheType} this configuration applies to.
         *
         * @return the {@link CacheType}.
         */
        public CacheType getType() {
            return type;
        }

        public void setType(CacheType type) {
            this.type = type;
        }

        /**
         * Gets the specific cache name for this model type.
         *
         * @return the cache name, or {@code null} if not specified.
         */
        public String getCacheName() {
            return cacheName;
        }

        public void setCacheName(String cacheName) {
            this.cacheName = cacheName;
        }

        /**
         * Gets the explicit JPA usage setting for this model type.
         *
         * @return {@link Boolean#TRUE} to enable JPA, {@link Boolean#FALSE} to disable,
         * or {@code null} to use the global/zen-mode default.
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
         * or {@code null} to use the global/zen-mode default.
         */
        public Boolean getUseCache() {
            return useCache;
        }

        public void setUseCache(Boolean useCache) {
            this.useCache = useCache;
        }

        /**
         * Gets the specific synchronization interval for this model type.
         *
         * @return the model-specific sync interval, or {@code null} if not specified.
         */
        public Duration getSyncInterval() {
            return syncInterval;
        }

        public void setSyncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
        }
    }
}