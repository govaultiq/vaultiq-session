package vaultiq.session.core;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface VaultiqSessionManager {

    VaultiqSession createSession(String userId, HttpServletRequest request);

    VaultiqSession getSession(String sessionId);

    void updateToCurrentlyActive(String sessionId);

    void deleteSession(String sessionId);

    List<VaultiqSession> getSessionsByUser(String userId);

    int totalUserSessions(String userId);

    // TODO:blocklist all user sessions -> Can be used to log out from all devices
    // TODO:blocklist all user sessions excluding string...-> Can be used to log out from all devices with exclusions
    // TODO:blocklist session by ID -> Can be used to log out from one device.

}