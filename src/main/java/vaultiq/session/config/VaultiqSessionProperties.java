package vaultiq.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("vaultiq.session.persistence")
public class VaultiqSessionProperties {
    private Jpa jpa;
    private Cache cache;

    public Jpa getJpa() {
        return jpa;
    }

    public void setJpa(Jpa jpa) {
        this.jpa = jpa;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public static class Jpa {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Cache {
        private boolean enabled;
        private String manager;
        private String sessionPool;
        private String blocklistPool;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getManager() {
            return manager;
        }

        public void setManager(String manager) {
            this.manager = manager;
        }

        public String getSessionPool() {
            return sessionPool;
        }

        public void setSessionPool(String sessionPool) {
            this.sessionPool = sessionPool;
        }

        public String getBlocklistPool() {
            return blocklistPool;
        }

        public void setBlocklistPool(String blocklistPool) {
            this.blocklistPool = blocklistPool;
        }
    }
}
