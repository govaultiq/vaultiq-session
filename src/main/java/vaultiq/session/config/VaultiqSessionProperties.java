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
        private boolean allowInFlightCacheManagement;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAllowInFlightCacheManagement() {
            return allowInFlightCacheManagement;
        }

        public void setAllowInFlightCacheManagement(boolean allowInFlightCacheManagement) {
            this.allowInFlightCacheManagement = allowInFlightCacheManagement;
        }
    }

    public static class ViaJpa {
        private boolean enabled;
        private boolean allowInFlightEntityCreation;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAllowInFlightEntityCreation() {
            return allowInFlightEntityCreation;
        }

        public void setAllowInFlightEntityCreation(boolean allowInFlightEntityCreation) {
            this.allowInFlightEntityCreation = allowInFlightEntityCreation;
        }
    }
}
