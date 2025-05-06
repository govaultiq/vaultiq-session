package vaultiq.session.core;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface VaultiqSessionManager {

    VaultiqSession createSession(String userId, HttpServletRequest request);

    VaultiqSession getSession(String sessionId);

    void deleteSession(String sessionId);

    List<VaultiqSession> getSessionsByUser(String userId);

    int totalUserSessions(String userId);

    // TODO: Get LastActive At by Session -> Can be used to check when the session was active last time.
    // TODO: Blocklist all user sessions -> Can be used to log out from all devices.
    // TODO: Blocklist all user sessions excluding string vararg -> Can be used to log out from all devices with exclusions.
    // TODO: Blocklist session by ID -> Can be used to log out from one device.
    // TODO: Check if the session blocklisted -> Can be used to check if a session is blocked.
    // TODO: Get All blocklisted sessions by user -> Can be used to getSession all blocked sessions for a user.
}