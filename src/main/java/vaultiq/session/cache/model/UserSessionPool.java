package vaultiq.session.cache.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserSessionPool implements Serializable {
    List<VaultiqSessionCacheEntry> sessions;

    public UserSessionPool(List<VaultiqSessionCacheEntry> sessions) {
        this.sessions = sessions;
    }

    public static UserSessionPool createEmpty() {
        return new UserSessionPool(new ArrayList<>());
    }

    public List<VaultiqSessionCacheEntry> getSessions() {
        return Optional.ofNullable(sessions).orElseGet(ArrayList::new);
    }

    public void setSessions(List<VaultiqSessionCacheEntry> sessions) {
        this.sessions = sessions;
    }

    public void addSession(VaultiqSessionCacheEntry session) {
        var didUpdate = this.updateSession(session);
        if(!didUpdate)
            sessions.add(session);
    }

    public int size() {
        return sessions.size();
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    public VaultiqSessionCacheEntry getSession(String sessionId) {
        for (VaultiqSessionCacheEntry session : sessions) {
            if (session.getSessionId().equals(sessionId)) {
                return session;
            }
        }
        return null;
    }

    public boolean updateSession(VaultiqSessionCacheEntry session) {
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getSessionId().equals(session.getSessionId())) {
                sessions.set(i, session);
                return true;
            }
        }
        return false;
    }

    public void deleteSession(String sessionId) {
        sessions.removeIf(session -> session.getSessionId().equals(sessionId));
    }
}
