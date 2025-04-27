package vaultiq.session.core;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface VaultiqSessionManager {

    VaultiqSession createSession(String userId, HttpServletRequest request);

    VaultiqSession getSession(String sessionId);

    void updateSession(VaultiqSession session);

    void deleteSession(String sessionId);

    List<VaultiqSession> getSessionsByUser(String userId);
}
