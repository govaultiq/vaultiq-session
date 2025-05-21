package vaultiq.session.config.annotation.model;

import vaultiq.session.core.model.ModelType;

/**
 * Enumeration defining the individual persistence methods that can be enabled
 * for a Vaultiq session data model.
 * <p>
 * This enum represents the specific storage mechanisms (cache or JPA) that can be
 * configured for a given {@link ModelType}. It is often used
 * in configuration properties or annotations (like {@link vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig})
 * to specify which method of persistence is active for a particular model.
 * </p>
 * <p>
 * Unlike {@link VaultiqPersistenceMode}, which describes the *combination* of methods used
 * (e.g., CACHE_ONLY, JPA_AND_CACHE), this enum represents the atomic options.
 * </p>
 *
 * @see vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig Annotation using this enum to condition beans.
 * @see ModelType Defines the different types of session data models.
 * @see VaultiqPersistenceMode Defines combinations of persistence methods.
 */
public enum VaultiqPersistenceMethod {
    /**
     * Indicates that caching is enabled and should be used for persistence
     * of a specific data model.
     */
    USE_CACHE,

    /**
     * Indicates that JPA (database persistence) is enabled and should be used
     * for persistence of a specific data model.
     */
    USE_JPA
}
