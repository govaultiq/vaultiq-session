package vaultiq.session.cache.model;

import java.util.ArrayList;
import java.util.List;

public class SessionIds {
    private List<String> sessions = new ArrayList<>();

    public List<String> getSessions() {
        return sessions;
    }

    public void setSessions(List<String> sessions) {
        this.sessions = sessions != null ? sessions : new ArrayList<>();
    }
}
