package vaultiq.session.core.model;

import vaultiq.session.core.SessionRevocationManager;

/**
 * Context object used to define the parameters of a session revoke operation.
 * <p>
 * This class encapsulates the necessary information for {@link SessionRevocationManager}
 * implementations to perform various types of session invalidation, such as
 * blocklisting a single session, all sessions for a user, or all sessions for a
 * user except specific ones.
 * </p>
 * <p>
 * Instances of this class are typically created using the static factory methods
 * and their fluent builder patterns: {@link #revoke()}, {@link #revokeAll()},
 * and {@link #revokeAllExcept()}.
 * </p>
 *
 * @see RevocationType
 * @see SessionRevocationManager
 */
public class RevocationRequest {

    private final RevocationType revocationType;
    private final String identifier;
    private final String note;
    private final String[] excludedSessionIds;

    public RevocationRequest(RevocationType revocationType, String identifier, String note) {
        this(revocationType, identifier, note, null);
    }

    public RevocationRequest(RevocationType revocationType, String identifier, String note, String[] excludedSessionIds) {
        this.revocationType = revocationType;
        this.identifier = identifier;
        this.note = note;
        this.excludedSessionIds = excludedSessionIds;
    }

    public RevocationType getRevocationType() {
        return revocationType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getNote() {
        return note;
    }

    public String[] getExcludedSessionIds() {
        return excludedSessionIds;
    }

    public static RevokeAllBuilder revokeAll() {
        return new RevokeAllBuilder(RevocationType.LOGOUT_ALL);
    }

    public static RevokeOneBuilder revoke() {
        return new RevokeOneBuilder(RevocationType.LOGOUT);
    }

    public static RevokeAllExceptBuilder revokeAllExcept() {
        return new RevokeAllExceptBuilder(RevocationType.LOGOUT_WITH_EXCLUSION);
    }

    /**
     * Builder for creating a {@link RevocationRequest} with {@link RevocationType#LOGOUT_ALL}.
     * <p>
     * Used to define a revoke operation that targets all sessions of a specific user.
     * </p>
     */
    public static class RevokeAllBuilder {
        private final RevocationType revocationType;
        private String identifier;

        private RevokeAllBuilder(RevocationType revocationType) {
            this.revocationType = revocationType;
        }

        public RevokeAllBuilder forUser(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public RevocationRequest withNote(String note) {
            return new RevocationRequest(revocationType, identifier, note);
        }

        public RevocationRequest noNote() {
            return new RevocationRequest(revocationType, identifier, null);
        }
    }

    /**
     * Builder for creating a {@link RevocationRequest} with {@link RevocationType#LOGOUT}.
     * <p>
     * Used to define a revoke operation that targets a single, specific session.
     * </p>
     */
    public static class RevokeOneBuilder {
        private final RevocationType revocationType;
        private String identifier;

        private RevokeOneBuilder(RevocationType revocationType) {
            this.revocationType = revocationType;
        }

        public RevokeOneBuilder withId(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public RevocationRequest withNote(String note) {
            return new RevocationRequest(revocationType, identifier, note);
        }

        public RevocationRequest noNote() {
            return new RevocationRequest(revocationType, identifier, null);
        }
    }

    /**
     * Builder for creating a {@link RevocationRequest} with {@link RevocationType#LOGOUT_WITH_EXCLUSION}.
     * <p>
     * Used to define a revoke operation that targets all sessions of a specific user,
     * explicitly excluding a provided list of session IDs.
     * </p>
     */
    public static class RevokeAllExceptBuilder {
        private final RevocationType revocationType;
        private String identifier;
        private String[] excludedSessionIds;

        private RevokeAllExceptBuilder(RevocationType revocationType) {
            this.revocationType = revocationType;
        }

        public RevokeAllExceptBuilder forUser(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public RevokeAllExceptBuilder excluding(String... excludedSessionIds) {
            this.excludedSessionIds = excludedSessionIds;
            return this;
        }

        public RevocationRequest withNote(String note) {
            return new RevocationRequest(revocationType, identifier, note, excludedSessionIds);
        }

        public RevocationRequest noNote() {
            return new RevocationRequest(revocationType, identifier, null, excludedSessionIds);
        }
    }
}
