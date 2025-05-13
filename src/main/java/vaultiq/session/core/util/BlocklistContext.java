package vaultiq.session.core.util;

import vaultiq.session.core.model.RevocationType;

/**
 * A context object representing a blocklist operation.
 * Used internally to describe revocation strategy and its parameters.
 */
public class BlocklistContext {

    private final RevocationType revocationType;
    private final String identifier;
    private final String note;
    private final String[] excludedSessionIds;

    public BlocklistContext(RevocationType revocationType, String identifier, String note) {
        this(revocationType, identifier, note, null);
    }

    public BlocklistContext(RevocationType revocationType, String identifier, String note, String[] excludedSessionIds) {
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

    /**
     * Creates a context builder for revoking all sessions of a user.
     *
     * @return builder for blocklisting all sessions
     */
    public static BlocklistAllContextBuilder blocklistAll() {
        return new BlocklistAllContextBuilder(RevocationType.LOGOUT_ALL);
    }

    /**
     * Creates a context builder for revoking a single session.
     *
     * @return builder for blocklisting a single session
     */
    public static BlocklistOneContextBuilder blocklist() {
        return new BlocklistOneContextBuilder(RevocationType.LOGOUT);
    }

    /**
     * Creates a context builder for revoking all sessions except specified ones.
     *
     * @return builder for blocklisting all sessions with exclusions
     */
    public static BlocklistAllExceptContextBuilder blocklistWithExclusions() {
        return new BlocklistAllExceptContextBuilder(RevocationType.LOGOUT_WITH_EXCLUSION);
    }

    /**
     * Builder for blocklisting all sessions of a user.
     */
    public static class BlocklistAllContextBuilder {
        private final RevocationType revocationType;
        private String identifier;

        private BlocklistAllContextBuilder(RevocationType revocationType) {
            this.revocationType = revocationType;
        }

        /**
         * Sets the user identifier (typically userId).
         */
        public BlocklistAllContextBuilder forUser(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Finalizes the context with a note.
         */
        public BlocklistContext withNote(String note) {
            return new BlocklistContext(revocationType, identifier, note);
        }

        /**
         * Finalizes the context with no note.
         */
        public BlocklistContext noNote() {
            return new BlocklistContext(revocationType, identifier, null);
        }
    }

    /**
     * Builder for blocklisting a single session.
     */
    public static class BlocklistOneContextBuilder {
        private final RevocationType revocationType;
        private String identifier;

        private BlocklistOneContextBuilder(RevocationType revocationType) {
            this.revocationType = revocationType;
        }

        /**
         * Sets the session identifier.
         */
        public BlocklistOneContextBuilder withId(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Finalizes the context with a note.
         */
        public BlocklistContext withNote(String note) {
            return new BlocklistContext(revocationType, identifier, note);
        }

        /**
         * Finalizes the context with no note.
         */
        public BlocklistContext noNote() {
            return new BlocklistContext(revocationType, identifier, null);
        }
    }

    /**
     * Builder for blocklisting all sessions of a user except specific ones.
     */
    public static class BlocklistAllExceptContextBuilder {
        private final RevocationType revocationType;
        private String identifier;
        private String[] excludedSessionIds;

        private BlocklistAllExceptContextBuilder(RevocationType revocationType) {
            this.revocationType = revocationType;
        }

        /**
         * Sets the user identifier (typically userId).
         */
        public BlocklistAllExceptContextBuilder forUser(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Specifies session IDs to exclude from revocation.
         */
        public BlocklistAllExceptContextBuilder excluding(String... excludedSessionIds) {
            this.excludedSessionIds = excludedSessionIds;
            return this;
        }

        /**
         * Finalizes the context with a note.
         */
        public BlocklistContext withNote(String note) {
            return new BlocklistContext(revocationType, identifier, note, excludedSessionIds);
        }

        /**
         * Finalizes the context with no note.
         */
        public BlocklistContext noNote() {
            return new BlocklistContext(revocationType, identifier, null, excludedSessionIds);
        }
    }
}
