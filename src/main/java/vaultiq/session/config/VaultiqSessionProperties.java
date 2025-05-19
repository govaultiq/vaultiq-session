package vaultiq.session.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.core.util.VaultiqModelConfigEnhancer;
import vaultiq.session.core.util.VaultiqSessionContext;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for the Vaultiq Session Tracking Library.
 * <p>
 * These properties allow configuring of {@code vaultiq-session}, particularly
 * concerning persistence strategies for different session data models. Properties
 * are bound from application configuration files (e.g., {@code application.properties},
 * {@code application.yml}) using the prefix {@code "vaultiq.session"}.
 * </p>
 * <p>
 * The configured properties are processed internally by the library (e.g.,
 * {@link VaultiqModelConfigEnhancer}) to determine the effective persistence
 * settings used by components like {@link VaultiqSessionContext}.
 * </p>
 */
@Configuration
@ConfigurationProperties("vaultiq.session")
public class VaultiqSessionProperties {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionProperties.class);

    public VaultiqSessionProperties() {
        log.info("Configuring vaultiq session environment.");
    }

    /**
     * Configures "zen mode" for session persistence.
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
    private boolean zenMode = false;
    private Persistence persistence;

    public boolean isZenMode() {
        return zenMode;
    }

    public void setZenMode(boolean zenMode) {
        this.zenMode = zenMode;
    }

    /**
     * Returns the persistence configuration settings.
     *
     * @return the persistence configuration.
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
     * This includes global cache settings and specific configurations per model type.
     * </p>
     */
    public static class Persistence {
        private CacheConfig cacheConfig;
        private List<ModelPersistenceConfig> models;

        /**
         * Returns the global cache configuration settings.
         * These settings apply unless overridden by {@link ModelPersistenceConfig}.
         *
         * @return the global cache configuration.
         */
        public CacheConfig getCacheConfig() {
            return cacheConfig == null ? new CacheConfig() : cacheConfig;
        }

        public void setCacheConfig(CacheConfig cacheConfig) {
            this.cacheConfig = cacheConfig;
        }

        /**
         * Returns the list of specific persistence configurations per model type.
         * These configurations override the global {@link CacheConfig}.
         *
         * @return a list of model-specific persistence configurations.
         */
        public List<ModelPersistenceConfig> getModels() {
            return models;
        }

        public void setModels(List<ModelPersistenceConfig> models) {
            this.models = models;
        }
    }

    /**
     * Global cache-specific configuration properties.
     * <p>
     * These settings apply to all model types unless overridden by a specific
     * {@link ModelPersistenceConfig}.
     * </p>
     */
    public static class CacheConfig {
        // These fields represent global default settings for using JPA and Cache.
        // The effective setting for a model type depends on zenMode, global config,
        // and model-specific overrides.
        private boolean useJpa = false;
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
         * The interval for cache synchronization operations, if supported and
         * applicable to the configured cache implementation.
         * <p>
         * Defaults to 5 minutes.
         * </p>
         */
        private Duration syncInterval = Duration.ofMinutes(5);

        public boolean isUseJpa() {
            return useJpa;
        }

        public void setUseJpa(boolean useJpa) {
            this.useJpa = useJpa;
        }

        public boolean isUseCache() {
            return useCache;
        }

        public void setUseCache(boolean useCache) {
            this.useCache = useCache;
        }

        public String getManager() {
            return manager;
        }

        public void setManager(String manager) {
            this.manager = manager;
        }

        public Duration getSyncInterval() {
            return syncInterval;
        }

        public void setSyncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
        }
    }

    /**
     * Persistence configuration for a specific {@link ModelType}.
     * <p>
     * Properties defined here override the global settings in {@link CacheConfig}
     * for the specified {@link #type}.
     * </p>
     */
    public static class ModelPersistenceConfig {
        /**
         * The {@link ModelType} to which this specific configuration applies.
         */
        private ModelType type;
        /**
         * The specific cache name to use for this model type.
         * If not specified, the library might fall back to a default name
         * based on the {@link ModelType}'s alias.
         */
        private String cacheName;
        /**
         * Explicitly enable or disable JPA usage for this model type.
         * If {@code null}, the global {@link CacheConfig#isUseJpa()} or
         * {@link VaultiqSessionProperties#isZenMode()} setting is used.
         */
        private Boolean useJpa;
        /**
         * Explicitly enable or disable Cache usage for this model type.
         * If {@code null}, the global {@link CacheConfig#isUseCache()} or
         * {@link VaultiqSessionProperties#isZenMode()} setting is used.
         */
        private Boolean useCache;
        /**
         * The sync interval specifically for this model type.
         * If {@code null}, the global {@link CacheConfig#getSyncInterval()} is used.
         */
        private Duration syncInterval;

        public ModelType getType() {
            return type;
        }

        public void setType(ModelType type) {
            this.type = type;
        }

        public String getCacheName() {
            return cacheName;
        }

        public void setCacheName(String cacheName) {
            this.cacheName = cacheName;
        }

        public Boolean getUseJpa() {
            return useJpa;
        }

        public void setUseJpa(Boolean useJpa) {
            this.useJpa = useJpa;
        }

        public Boolean getUseCache() {
            return useCache;
        }

        public void setUseCache(Boolean useCache) {
            this.useCache = useCache;
        }

        public Duration getSyncInterval() {
            return syncInterval;
        }

        public void setSyncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
        }
    }
}
