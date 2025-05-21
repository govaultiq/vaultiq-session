package vaultiq.session.core.model;

/**
 * Enumeration of the session-related model types provided by the Vaultiq-session library.
 * <p>
 * Used in configuration to specify which data model (SESSION, USER_SESSION_MAPPING,
 * USER_ACTIVITY_LOGS, or REVOKE) should use which persistence or caching strategy.
 * </p>
 */
public enum ModelType {
    /** Active session objects. */
    SESSION,

    /** Mapping of user IDs to their session IDs. */
    USER_SESSION_MAPPING,

    /** User activity and audit logs. */
    USER_ACTIVITY_LOGS,

    /** Revoked or invalidated session identifiers. */
    REVOKE
}
