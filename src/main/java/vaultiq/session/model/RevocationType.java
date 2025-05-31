package vaultiq.session.model;

/**
 * Enumeration defining the different types or strategies of session revocation (revoking).
 * <p>
 * This enum is used to categorize the reason or method by which a session was
 * added to the revoke. It provides context for the revoke entry, indicating
 * whether a single session was revoked, all sessions for a user were revoked,
 * or all sessions except certain ones were revoked.
 * </p>
 * <p>
 * Used within {@link RevokedSession} to record the type of revocation and in
 * {@link RevocationRequest} to specify the intended
 * revoke operation.
 * </p>
 *
 * @see RevokedSession
 * @see RevocationRequest
 */
public enum RevocationType {
    /**
     * Indicates that a single, specific session was revoked.
     * This typically corresponds to a standard user-initiated logout of one session.
     */
    LOGOUT,
    /**
     * Indicates that all sessions belonging to a specific user were revoked,
     * except one or more specified sessions.
     * This is often used for scenarios like "log out all other devices".
     */
    LOGOUT_WITH_EXCLUSION,
    /**
     * Indicates that all sessions belonging to a specific user were revoked.
     * This typically corresponds to a forced logout of a user from all their active sessions.
     */
    LOGOUT_ALL;
}
