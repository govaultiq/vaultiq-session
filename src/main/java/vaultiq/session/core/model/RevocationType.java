package vaultiq.session.core.model;

/**
 * Enumeration defining the different types or strategies of session revocation (blocklisting).
 * <p>
 * This enum is used to categorize the reason or method by which a session was
 * added to the blocklist. It provides context for the blocklist entry, indicating
 * whether a single session was blocklisted, all sessions for a user were blocklisted,
 * or all sessions except certain ones were blocklisted.
 * </p>
 * <p>
 * Used within {@link SessionBlocklist} to record the type of revocation and in
 * {@link vaultiq.session.core.util.BlocklistContext} to specify the intended
 * blocklist operation.
 * </p>
 *
 * @see SessionBlocklist
 * @see vaultiq.session.core.util.BlocklistContext
 */
public enum RevocationType {
    /**
     * Indicates that a single, specific session was blocklisted.
     * This typically corresponds to a standard user-initiated logout of one session.
     */
    LOGOUT,
    /**
     * Indicates that all sessions belonging to a specific user were blocklisted,
     * except one or more specified sessions.
     * This is often used for scenarios like "log out all other devices".
     */
    LOGOUT_WITH_EXCLUSION,
    /**
     * Indicates that all sessions belonging to a specific user were blocklisted.
     * This typically corresponds to a forced logout of a user from all their active sessions.
     */
    LOGOUT_ALL;
}
