package vaultiq.session.cache.util;

import vaultiq.session.core.model.ModelType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Utility for default cache names associated with each ModelType.
 */
public final class DefaultCacheNames {

    private static final Map<ModelType, String> DEFAULT_NAMES = new EnumMap<>(ModelType.class);

    static {
        DEFAULT_NAMES.put(ModelType.SESSION, "vaultiq-session-pool");
        DEFAULT_NAMES.put(ModelType.USER_SESSION_MAPPING, "vaultiq-user-session-mapping");
        DEFAULT_NAMES.put(ModelType.USER_ACTIVITY_LOGS, "vaultiq-user-activity-logs");
        DEFAULT_NAMES.put(ModelType.REVOKE, "vaultiq-revoked-session-pool");
    }

    private DefaultCacheNames() {
        // Utility class
    }

    /**
     * Returns the default cache name for the given model type.
     */
    public static String get(ModelType type) {
        return DEFAULT_NAMES.get(type);
    }
}
