package vaultiq.session.core.model;

/**
 * Enumeration of the session-related model types provided by the Vaultiq-session library.
 * <p>
 * Used in configuration to specify which data model (SESSION or REVOKE)
 * should use which persistence or caching strategy.
 * </p>
 */
public enum ModelType {
    /** Device Session model type. */
    SESSION,

    /** Revoked or invalidated session model type. */
    REVOKE
}
