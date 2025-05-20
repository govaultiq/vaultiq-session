package vaultiq.session.context;

import vaultiq.session.core.model.RevocationType;
import vaultiq.session.core.SessionBlocklistManager;

/**
 * Context object used to define the parameters of a session blocklist operation.
 * <p>
 * This class encapsulates the necessary information for {@link SessionBlocklistManager}
 * implementations to perform various types of session invalidation, such as
 * blocklisting a single session, all sessions for a user, or all sessions for a
 * user except specific ones.
 * </p>
 * <p>
 * Instances of this class are typically created using the static factory methods
 * and their fluent builder patterns: {@link #blocklist()}, {@link #blocklistAll()},
 * and {@link #blocklistWithExclusions()}.
 * </p>
 *
 * @see RevocationType
 * @see SessionBlocklistManager
 */
public class BlocklistContext {

    private final RevocationType revocationType;
    private final String identifier; // Can be sessionId or userId depending on RevocationType
    private final String note;
    private final String[] excludedSessionIds; // Used only for LOGOUT_WITH_EXCLUSION

    /**
     * Constructs a new BlocklistContext.
     * <p>
     * This constructor is primarily used internally by the static factory methods
     * and their builders. Users should prefer the fluent builder API.
     * </p>
     *
     * @param revocationType The type of revocation operation.
     * @param identifier     The primary identifier for the operation (e.g., user ID for LOGOUT_ALL, session ID for LOGOUT).
     * @param note           An optional note providing context for the blocklist action.
     */
    public BlocklistContext(RevocationType revocationType, String identifier, String note) {
        this(revocationType, identifier, note, null);
    }

    /**
     * Constructs a new BlocklistContext, including excluded session IDs.
     * <p>
     * This constructor is primarily used internally by the static factory methods
     * and their builders, specifically for the {@link RevocationType#LOGOUT_WITH_EXCLUSION} type.
     * Users should prefer the fluent builder API.
     * </p>
     *
     * @param revocationType     The type of revocation operation (expected to be {@link RevocationType#LOGOUT_WITH_EXCLUSION}).
     * @param identifier         The primary identifier for the operation (e.g., user ID).
     * @param note               An optional note providing context for the blocklist action.
     * @param excludedSessionIds An array of session IDs to exclude from the blocklist operation.
     */
    public BlocklistContext(RevocationType revocationType, String identifier, String note, String[] excludedSessionIds) {
        this.revocationType = revocationType;
        this.identifier = identifier;
        this.note = note;
        this.excludedSessionIds = excludedSessionIds;
    }

    /**
     * Returns the type of revocation operation defined by this context.
     *
     * @return the {@link RevocationType}.
     */
    public RevocationType getRevocationType() {
        return revocationType;
    }

    /**
     * Returns the primary identifier associated with this blocklist operation.
     * <p>
     * The meaning of this identifier depends on the {@link #getRevocationType()}:
     * <ul>
     * <li>For {@link RevocationType#LOGOUT_ALL} and {@link RevocationType#LOGOUT_WITH_EXCLUSION}, this is typically the user ID.</li>
     * <li>For {@link RevocationType#LOGOUT}, this is the specific session ID to blocklist.</li>
     * </ul>
     * </p>
     *
     * @return the primary identifier string.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the optional note providing additional context for the blocklist action.
     *
     * @return the note string, or {@code null} if no note was provided.
     */
    public String getNote() {
        return note;
    }

    /**
     * Returns the array of session IDs to be excluded from the blocklist operation.
     * <p>
     * This is only relevant for {@link RevocationType#LOGOUT_WITH_EXCLUSION}. For other
     * {@link RevocationType}s, this will be {@code null}.
     * </p>
     *
     * @return an array of excluded session IDs, or {@code null}.
     */
    public String[] getExcludedSessionIds() {
        return excludedSessionIds;
    }

    /**
     * Starts the fluent builder process for creating a context to blocklist
     * all sessions belonging to a specific user.
     *
     * @return a {@link BlocklistAllContextBuilder} instance.
     */
    public static BlocklistAllContextBuilder blocklistAll() {
        return new BlocklistAllContextBuilder(RevocationType.LOGOUT_ALL);
    }

    /**
     * Starts the fluent builder process for creating a context to blocklist
     * a single, specific session.
     *
     * @return a {@link BlocklistOneContextBuilder} instance.
     */
    public static BlocklistOneContextBuilder blocklist() {
        return new BlocklistOneContextBuilder(RevocationType.LOGOUT);
    }

    /**
     * Starts the fluent builder process for creating a context to blocklist
     * all sessions belonging to a user, except for a specified list of sessions.
     *
     * @return a {@link BlocklistAllExceptContextBuilder} instance.
     */
    public static BlocklistAllExceptContextBuilder blocklistWithExclusions() {
        return new BlocklistAllExceptContextBuilder(RevocationType.LOGOUT_WITH_EXCLUSION);
    }

    /**
     * Builder for creating a {@link BlocklistContext} with {@link RevocationType#LOGOUT_ALL}.
     * <p>
     * Used to define a blocklist operation that targets all sessions of a specific user.
     * </p>
     */
    public static class BlocklistAllContextBuilder {
        private final RevocationType revocationType;
        private String identifier;

        private BlocklistAllContextBuilder(RevocationType revocationType) {
            this.revocationType = revocationType;
        }

        /**
         * Specifies the user ID whose sessions should all be blocklisted.
         *
         * @param identifier the unique identifier of the user. Must not be blank.
         * @return the current builder instance for chaining.
         */
        public BlocklistAllContextBuilder forUser(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Finalizes the builder and creates the {@link BlocklistContext} with an
         * optional note.
         *
         * @param note an optional note for the blocklist action.
         * @return the constructed {@link BlocklistContext}.
         */
        public BlocklistContext withNote(String note) {
            return new BlocklistContext(revocationType, identifier, note);
        }

        /**
         * Finalizes the builder and creates the {@link BlocklistContext} with no note.
         *
         * @return the constructed {@link BlocklistContext}.
         */
        public BlocklistContext noNote() {
            return new BlocklistContext(revocationType, identifier, null);
        }
    }

    /**
     * Builder for creating a {@link BlocklistContext} with {@link RevocationType#LOGOUT}.
     * <p>
     * Used to define a blocklist operation that targets a single, specific session.
     * </p>
     */
    public static class BlocklistOneContextBuilder {
        private final RevocationType revocationType;
        private String identifier;

        private BlocklistOneContextBuilder(RevocationType revocationType) {
            this.revocationType = revocationType;
        }

        /**
         * Specifies the session ID to be blocklisted.
         *
         * @param identifier the unique identifier of the session. Must not be blank.
         * @return the current builder instance for chaining.
         */
        public BlocklistOneContextBuilder withId(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Finalizes the builder and creates the {@link BlocklistContext} with an
         * optional note.
         *
         * @param note an optional note for the blocklist action.
         * @return the constructed {@link BlocklistContext}.
         */
        public BlocklistContext withNote(String note) {
            return new BlocklistContext(revocationType, identifier, note);
        }

        /**
         * Finalizes the builder and creates the {@link BlocklistContext} with no note.
         *
         * @return the constructed {@link BlocklistContext}.
         */
        public BlocklistContext noNote() {
            return new BlocklistContext(revocationType, identifier, null);
        }
    }

    /**
     * Builder for creating a {@link BlocklistContext} with {@link RevocationType#LOGOUT_WITH_EXCLUSION}.
     * <p>
     * Used to define a blocklist operation that targets all sessions of a specific user,
     * explicitly excluding a provided list of session IDs.
     * </p>
     */
    public static class BlocklistAllExceptContextBuilder {
        private final RevocationType revocationType;
        private String identifier;
        private String[] excludedSessionIds;

        private BlocklistAllExceptContextBuilder(RevocationType revocationType) {
            this.revocationType = revocationType;
        }

        /**
         * Specifies the user ID whose sessions should be blocklisted, with exclusions.
         *
         * @param identifier the unique identifier of the user. Must not be blank.
         * @return the current builder instance for chaining.
         */
        public BlocklistAllExceptContextBuilder forUser(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Specifies the session IDs that should *not* be blocklisted.
         * All other sessions for the user specified by {@link #forUser(String)}
         * will be blocklisted.
         *
         * @param excludedSessionIds a varargs array of session IDs to exclude. Can be empty.
         * @return the current builder instance for chaining.
         */
        public BlocklistAllExceptContextBuilder excluding(String... excludedSessionIds) {
            this.excludedSessionIds = excludedSessionIds;
            return this;
        }

        /**
         * Finalizes the builder and creates the {@link BlocklistContext} with an
         * optional note.
         *
         * @param note an optional note for the blocklist action.
         * @return the constructed {@link BlocklistContext}.
         */
        public BlocklistContext withNote(String note) {
            return new BlocklistContext(revocationType, identifier, note, excludedSessionIds);
        }

        /**
         * Finalizes the builder and creates the {@link BlocklistContext} with no note.
         *
         * @return the constructed {@link BlocklistContext}.
         */
        public BlocklistContext noNote() {
            return new BlocklistContext(revocationType, identifier, null, excludedSessionIds);
        }
    }
}
