package vaultiq.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties("vaultiq.session.persistence")
public class VaultiqSessionProperties {
    private Jpa jpa;
    private Cache cache;
    private Duration syncInterval = Duration.ofMinutes(5);

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

    public Duration getSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(Duration syncInterval) {
        this.syncInterval = syncInterval;
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
        private CacheNames cacheNames;

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

        public CacheNames getCacheNames() {
            return cacheNames;
        }

        public void setCacheNames(CacheNames cacheNames) {
            this.cacheNames = cacheNames;
        }
    }

    public static class CacheNames {
        private String sessions = "session-pool";
        private String userSessionMapping = "user-session-mapping";
        private String lastActiveTimestamps = "last-active-timestamps";
        private String blocklist = "blacklist";

        public String getSessions() {
            return sessions;
        }

        public void setSessions(String sessions) {
            this.sessions = sessions;
        }

        public String getUserSessionMapping() {
            return userSessionMapping;
        }

        public void setUserSessionMapping(String userSessionMapping) {
            this.userSessionMapping = userSessionMapping;
        }

        public String getLastActiveTimestamps() {
            return lastActiveTimestamps;
        }

        public void setLastActiveTimestamps(String lastActiveTimestamps) {
            this.lastActiveTimestamps = lastActiveTimestamps;
        }

        public String getBlocklist() {
            return blocklist;
        }

        public void setBlocklist(String blocklist) {
            this.blocklist = blocklist;
        }
    }
}
