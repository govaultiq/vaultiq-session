
package vaultiq.session.cache.model;

import vaultiq.session.context.VaultiqSessionContext;

/**
 * Enumeration of the major session-related model types supported by the Vaultiq session platform.
 * <p>
 * This enum enables type-safe configuration and lookup of persistence strategies (cache/JPA) for each data model.
 * It is used extensively by {@link VaultiqSessionContext} and {@link vaultiq.session.config.VaultiqSessionProperties}
 * to determine how each type of session data is persisted, cached, or revoked.
 * </p>
 *
 * <ul>
 *   <li>{@link #SESSION} - Standard in-memory session objects (active sessions).</li>
 *   <li>{@link #USER_SESSION_MAPPING} - Mapping of user identifiers to their associated session IDs.</li>
 *   <li>{@link #USER_ACTIVITY_LOGS} - Activity and audit logs relating to user actions in the session context.</li>
 *   <li>{@link #REVOKE} - List of blocklisted/invalidated session IDs to enforce revocation across subsystems.</li>
 * </ul>
 *
 * <p>
 * Each enum constant is associated with an alias, useful for descriptive configuration (e.g., persistence or cache property mapping).
 * </p>
 */
public enum ModelType {
    /**
     * Represents the model holding primary session objects (live session pool).
     */
    SESSION("session-pool"),

    /**
     * Represents the mapping between users and their active session IDs.
     */
    USER_SESSION_MAPPING("user-session-mapping"),

    /**
     * Represents a model tracking user-specific activity logs.
     */
    USER_ACTIVITY_LOGS("user-activity-logs"),

    /**
     * Represents the model tracking blocklisted (revoked) sessions.
     */
    REVOKE("revoke");

    private final String alias;

    /**
     * Constructs the model type with its descriptive alias.
     *
     * @param alias a string alias for this model type (used in configuration)
     */
    ModelType(String alias) {
        this.alias = alias;
    }

    /**
     * Returns the alias string for this model type.
     *
     * @return the configured alias of the model type.
     */
    public String getAlias() {
        return alias;
    }
}
