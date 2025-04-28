package vaultiq.session.redis.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserSessionPool implements Serializable {
    List<RedisVaultiqSession> sessions;

    public UserSessionPool(List<RedisVaultiqSession> sessions) {
        this.sessions = sessions;
    }

    public static UserSessionPool createEmpty() {
        return new UserSessionPool(new ArrayList<>());
    }

    public List<RedisVaultiqSession> getSessions() {
        return Optional.ofNullable(sessions).orElseGet(ArrayList::new);
    }

    public void setSessions(List<RedisVaultiqSession> sessions) {
        this.sessions = sessions;
    }

    public void addSession(RedisVaultiqSession session) {
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

    public RedisVaultiqSession getSession(String sessionId) {
        for (RedisVaultiqSession session : sessions) {
            if (session.getSessionId().equals(sessionId)) {
                return session;
            }
        }
        return null;
    }

    public boolean updateSession(RedisVaultiqSession session) {
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
