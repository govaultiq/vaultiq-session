package vaultiq.session.redis.service;

public class RedisCacheContext {
    private static final ThreadLocal<String> currentCacheName = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();

    public static void setContext(String cacheName, String userId) {
        currentCacheName.set(cacheName);
        currentUserId.set(userId);
    }

    public static void setCacheName(String cacheName) {
        currentCacheName.set(cacheName);
    }

    public static String getCacheName() {
        return currentCacheName.get();
    }

    public static void setUserId(String userId) {
        currentUserId.set(userId);
    }

    public static String getUserId() {
        return currentUserId.get();
    }

    public static void clear() {
        currentCacheName.remove();
        currentUserId.remove();
    }
}
