package vaultiq.session.cache.model;

import java.util.HashSet;
import java.util.Set;

public class SessionIds {
    private Set<String> sessionIds = new HashSet<>();

    public Set<String> getSessionIds() {
        return sessionIds;
    }

    public void setSessionIds(Set<String> sessionIds) {
        this.sessionIds = sessionIds != null ? sessionIds : new HashSet<>();
    }

    public void addSessionId(String sessionId) {
        sessionIds.add(sessionId);
    }
}
