package vaultiq.session.cache.util;

import vaultiq.session.core.model.ModelType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents the different types of caches used in the vaultiq session module.
 * Each cache type has an associated alias and a ModelType.
 */
public enum CacheType {

    /**
     * Cache type for session fingerprints.
     */
    SESSION_FINGERPRINTS("session-fingerprints", ModelType.SESSION),
    /**
     * Cache type for the session pool.
     */
    SESSION_POOL("session-pool", ModelType.SESSION),
    /**
     * Cache type for user-session mappings.
     */
    USER_SESSION_MAPPINGS("user-session-mappings", ModelType.SESSION),
    /**
     * Cache type for revoked SIDs (Session IDs).
     */
    REVOKED_SIDS("revoked-sids", ModelType.REVOKE),
    /**
     * Cache type for the revoked session pool.
     */
    REVOKED_SESSION_POOL("revoked-session-pool", ModelType.REVOKE);

    private final String alias;
    private final ModelType type;

    // A static map for efficient lookup of CacheType by its alias (case-insensitive)
    private static final Map<String, CacheType> ALIAS_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(ct -> ct.alias.toLowerCase(), Function.identity()));

    // A static map for efficient lookup of CacheTypes by ModelType
    private static final Map<ModelType, Set<CacheType>> MODEL_TYPE_MAP =
            Collections.unmodifiableMap(Arrays.stream(values())
                    .collect(Collectors.groupingBy(
                            CacheType::modelType,
                            Collectors.toSet()
                    )));

    /**
     * Constructs a CacheType enum constant.
     *
     * @param alias The string alias for the cache type.
     * @param type  The ModelType associated with this cache type.
     */
    CacheType(String alias, ModelType type) {
        this.alias = alias;
        this.type = type;
    }

    /**
     * Returns the alias of this cache type.
     *
     * @return The alias string.
     */
    public String alias() {
        return alias;
    }

    /**
     * Returns the ModelType associated with this cache type.
     *
     * @return The ModelType.
     */
    public ModelType modelType() {
        return type;
    }

    /**
     * Parses a string alias to its corresponding CacheType.
     * The lookup is case-insensitive.
     *
     * @param alias The alias string to parse.
     * @return An {@link Optional} containing the CacheType if found, otherwise an empty Optional.
     */
    public static Optional<CacheType> parse(String alias) {
        return Optional.ofNullable(ALIAS_MAP.get(alias.toLowerCase()));
    }

    /**
     * Returns a set of CacheType enums associated with a given ModelType.
     *
     * @param type The ModelType to filter by.
     * @return A {@link Set} of CacheType enums matching the given ModelType.
     * Returns an empty set if no CacheTypes are associated with the ModelType.
     */
    public static Set<CacheType> getByModelType(ModelType type) {
        return MODEL_TYPE_MAP.getOrDefault(type, Collections.emptySet());
    }
}