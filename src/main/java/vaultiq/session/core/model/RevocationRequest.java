package vaultiq.session.core.model;

import vaultiq.session.core.SessionRevocationManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Immutable context object used to define parameters for session revocation.
 * <p>
 * This object is constructed via static factory methods that allow for different revocation
 * strategies while ensuring immutability and clarity of intent.
 * </p>
 * <p>
 * Supported revocation patterns include:
 * <ul>
 *     <li>{@link #revoke(String)} — Revokes a specific session by ID</li>
 *     <li>{@link #revokeAll()} — Revokes all sessions for a specific user or the current user</li>
 *     <li>{@link #revokeAllExcept(String...)} — Revokes all sessions except the provided ones</li>
 * </ul>
 * </p>
 * <p>
 * This API is intended for use by {@link SessionRevocationManager} implementations and follows a
 * fail-silent approach for non-critical inputs. A {@link NullPointerException} is thrown only if
 * a required session ID or user ID is null.
 * </p>
 *
 * @see SessionRevocationManager
 * @see RevocationType
 */
public final class RevocationRequest {

    private final RevocationType revocationType;
    private final boolean forCurrentUser;
    private final String identifier;
    private final String note;
    private final Set<String> excludedSessionIds;

    private RevocationRequest(RevocationType revocationType,
                              boolean forCurrentUser,
                              String identifier,
                              String note,
                              Set<String> excludedSessionIds) {
        this.revocationType = Objects.requireNonNull(revocationType, "Revocation type must not be null");
        this.forCurrentUser = forCurrentUser;
        this.identifier = forCurrentUser ? null : identifier;
        this.note = note;
        this.excludedSessionIds = excludedSessionIds;
    }

    public RevocationType getRevocationType() {
        return revocationType;
    }

    public boolean isForCurrentUser() {
        return forCurrentUser;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getNote() {
        return note;
    }

    public Set<String> getExcludedSessionIds() {
        return excludedSessionIds;
    }

    /**
     * Start a single-session revoke request.
     *
     * @param sessionId must not be null
     * @throws NullPointerException if sessionId is null
     */
    public static RevokeOneBuilder revoke(String sessionId) {
        return new RevokeOneBuilder(Objects.requireNonNull(sessionId, "Session ID cannot be null"));
    }

    /**
     * Start a revoke-all request (by user or current user).
     */
    public static RevokeAllBuilder revokeAll() {
        return new RevokeAllBuilder();
    }

    /**
     * Start a revoke-all-except request with initial exclusions.
     *
     * @param excludedSessionIds session IDs to exclude
     */
    public static RevokeAllExceptBuilder revokeAllExcept(String... excludedSessionIds) {
        Collection<String> list = excludedSessionIds != null
                ? Arrays.asList(excludedSessionIds)
                : Collections.emptyList();
        Set<String> cleaned = clean(list);
        return new RevokeAllExceptBuilder(cleaned);
    }

    /**
     * Start a revoke-all-except request with initial exclusions.
     *
     * @param excludedSessionIds session IDs to exclude
     */
    public static RevokeAllExceptBuilder revokeAllExcept(Set<String> excludedSessionIds) {
        Set<String> cleaned = clean(excludedSessionIds);
        return new RevokeAllExceptBuilder(cleaned);
    }

    private static Set<String> clean(Collection<String> input) {
        if (input == null) {
            return Collections.emptySet();
        }
        return Set.copyOf(input.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet()));
    }

    public static final class RevokeOneBuilder {
        private final String sessionId;
        private String note;

        private RevokeOneBuilder(String sessionId) {
            this.sessionId = sessionId;
        }

        public RevokeOneBuilder withNote(String note) {
            this.note = note;
            return this;
        }

        public RevocationRequest build() {
            return new RevocationRequest(RevocationType.LOGOUT,
                    false,
                    sessionId,
                    note,
                    null);
        }
    }

    public static final class RevokeAllBuilder {
        private String identifier;
        private boolean forCurrentUser;
        private String note;

        private RevokeAllBuilder() {
        }

        public RevokeAllBuilder forUser(String identifier) {
            this.identifier = Objects.requireNonNull(identifier, "User ID cannot be null");
            return this;
        }

        public RevokeAllBuilder forCurrentUser() {
            this.forCurrentUser = true;
            return this;
        }

        public RevokeAllBuilder withNote(String note) {
            this.note = note;
            return this;
        }

        public RevocationRequest build() {
            return new RevocationRequest(RevocationType.LOGOUT_ALL,
                    forCurrentUser,
                    identifier,
                    note,
                    null);
        }
    }

    public static final class RevokeAllExceptBuilder {
        private String identifier;
        private boolean forCurrentUser;
        private final Set<String> excludedSessionIds;
        private String note;

        private RevokeAllExceptBuilder(Set<String> excludedSessionIds) {
            this.excludedSessionIds = excludedSessionIds;
        }

        public RevokeAllExceptBuilder forUser(String identifier) {
            this.identifier = Objects.requireNonNull(identifier, "User ID cannot be null");
            return this;
        }

        public RevokeAllExceptBuilder forCurrentUser() {
            this.forCurrentUser = true;
            return this;
        }

        public RevokeAllExceptBuilder withNote(String note) {
            this.note = note;
            return this;
        }

        public RevocationRequest build() {
            return new RevocationRequest(RevocationType.LOGOUT_WITH_EXCLUSION,
                    forCurrentUser,
                    identifier,
                    note,
                    excludedSessionIds);
        }
    }
}
