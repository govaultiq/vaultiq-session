package vaultiq.session.redis.service;

public class RedisCacheContext {
    private static final ThreadLocal<String> currentCacheName = new ThreadLocal<>();

    public static void setCacheName(String cacheName) {
        currentCacheName.set(cacheName);
    }

    public static String getCacheName() {
        return currentCacheName.get();
    }

    public static void clear() {
        currentCacheName.remove();
    }
}
