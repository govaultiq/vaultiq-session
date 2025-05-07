package vaultiq.session.utility;

import jakarta.servlet.http.HttpServletRequest;

public class VaultiqRequestContext {
    private static final String VAULTIQ_SID_KEY = "vaultiq.sid";

    public static void setSessionId(HttpServletRequest request, String sessionId) {
        request.setAttribute(VAULTIQ_SID_KEY, sessionId);
    }

    public static String getSessionId(HttpServletRequest request) {
        Object attr = request.getAttribute(VAULTIQ_SID_KEY);
        return attr instanceof String ? (String) attr : null;
    }
}
