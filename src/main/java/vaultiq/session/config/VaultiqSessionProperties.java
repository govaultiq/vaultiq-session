package vaultiq.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("vaultiq.session.persistence")
public class VaultiqSessionProperties {
    private ViaRedis viaRedis;
    private ViaJpa viaJpa;

    public ViaRedis getViaRedis() {
        return viaRedis;
    }

    public void setViaRedis(ViaRedis viaRedis) {
        this.viaRedis = viaRedis;
    }

    public ViaJpa getViaJpa() {
        return viaJpa;
    }

    public void setViaJpa(ViaJpa viaJpa) {
        this.viaJpa = viaJpa;
    }

    public static class ViaRedis {
        private boolean enabled;
        private boolean allowInflightCacheManagement;
        private String cacheManagerName;
        private String cacheName;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAllowInflightCacheManagement() {
            return allowInflightCacheManagement;
        }

        public void setAllowInflightCacheManagement(boolean allowInflightCacheManagement) {
            this.allowInflightCacheManagement = allowInflightCacheManagement;
        }

        public String getCacheManagerName() {
            return cacheManagerName;
        }

        public void setCacheManagerName(String cacheManagerName) {
            this.cacheManagerName = cacheManagerName;
        }

        public String getCacheName() {
            return cacheName;
        }

        public void setCacheName(String cacheName) {
            this.cacheName = cacheName;
        }
    }

    public static class ViaJpa {
        private boolean enabled;
        private boolean allowInflightEntityCreation;
        private boolean enableCaching;
        private String cacheManagerName;
        private String cacheName;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAllowInflightEntityCreation() {
            return allowInflightEntityCreation;
        }

        public void setAllowInflightEntityCreation(boolean allowInflightEntityCreation) {
            this.allowInflightEntityCreation = allowInflightEntityCreation;
        }

        public boolean isEnableCaching() {
            return enableCaching;
        }

        public void setEnableCaching(boolean enableCaching) {
            this.enableCaching = enableCaching;
        }

        public String getCacheManagerName() {
            return cacheManagerName;
        }

        public void setCacheManagerName(String cacheManagerName) {
            this.cacheManagerName = cacheManagerName;
        }

        public String getCacheName() {
            return cacheName;
        }

        public void setCacheName(String cacheName) {
            this.cacheName = cacheName;
        }
    }
}
