package vaultiq.session.cache.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class SessionIds {
    private Set<String> sessionIds = new HashSet<>();
    private long lastUpdated; // epoch millis

    public Set<String> getSessionIds() {
        return sessionIds;
    }

    public void setSessionIds(Set<String> sessionIds) {
        this.sessionIds = sessionIds != null ? sessionIds : new HashSet<>();
        updatedNow();
    }

    public void addSessionId(String sessionId) {
        sessionIds.add(sessionId);
        updatedNow();
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    private void updatedNow() {
        this.lastUpdated = Instant.now().toEpochMilli();
    }
}
