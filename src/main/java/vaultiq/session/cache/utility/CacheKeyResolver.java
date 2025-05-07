package vaultiq.session.cache.utility;

public final class CacheKeyResolver {

    private CacheKeyResolver() {
        // avoiding external instantiation
    }

    public static String keyForSession(String sessionId) {
        return "session-pool-" + sessionId;
    }

    public static String keyForUserSessionMapping(String userId) {
        return "user-sessions-" + userId;
    }

    public static String keyForLastActiveTimestamp(String userId) {
        return "last-active-" + userId;
    }

    public static String keyForBlacklist(String sessionId) {
        return "blacklist-" + sessionId;
    }

}
