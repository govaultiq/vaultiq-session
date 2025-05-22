package vaultiq.session.cache.util;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CacheType {

    SESSION_FINGERPRINTS("session-fingerprints"),
    SESSION_POOL("session-pool"),
    USER_SESSION_MAPPINGS("user-session-mappings"),
    REVOKED_SIDS("revoked-sids"),
    REVOKED_SESSION_POOL("revoked-session-pool");

    private final String alias;

    CacheType(String alias) {
        this.alias = alias;
    }

    public String alias() {
        return alias;
    }

    private static final Map<String, CacheType> ALIAS_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(ct -> ct.alias.toLowerCase(), Function.identity()));

    public static CacheType parse(String alias) {
        CacheType type = ALIAS_MAP.get(alias.toLowerCase());
        if (type == null) throw new IllegalArgumentException("Invalid alias for CacheType: " + alias);
        return type;
    }

}

