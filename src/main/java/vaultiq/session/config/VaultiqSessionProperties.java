package vaultiq.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import vaultiq.session.cache.model.ModelType;

import java.time.Duration;
import java.util.List;

@Component
@ConfigurationProperties("vaultiq.session")
public class VaultiqSessionProperties {

    private boolean zenMode = false;
    private Persistence persistence;

    public boolean isZenMode() {
        return zenMode;
    }

    public void setZenMode(boolean zenMode) {
        this.zenMode = zenMode;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence == null ? new Persistence() : persistence;
    }

    public static class Persistence {
        private CacheConfig cacheConfig;
        private List<ModelPersistenceConfig> models;

        public CacheConfig getCacheConfig() {
            return cacheConfig == null ? new CacheConfig() : cacheConfig;
        }

        public void setCacheConfig(CacheConfig cacheConfig) {
            this.cacheConfig = cacheConfig;
        }

        public List<ModelPersistenceConfig> getModels() {
            return models;
        }

        public void setModels(List<ModelPersistenceConfig> models) {
            this.models = models;
        }
    }

    public static class CacheConfig {
        private boolean useJpa = false;
        private boolean useCache = false;
        private String manager = "cacheManager";
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

    public static class ModelPersistenceConfig {
        private ModelType type;
        private String cacheName;
        private Boolean useJpa;
        private Boolean useCache;
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
