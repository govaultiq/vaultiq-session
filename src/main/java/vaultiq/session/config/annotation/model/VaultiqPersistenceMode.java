package vaultiq.session.config.annotation.model;

/**
 * Enumeration defining the possible persistence modes for Vaultiq session data models.
 * <p>
 * These modes are used in conjunction with the {@link vaultiq.session.config.annotation.ConditionalOnVaultiqPersistence}
 * annotation and {@link vaultiq.session.config.rules.VaultiqPersistenceModeCondition}
 * to conditionally enable beans based on how specific {@link vaultiq.session.cache.model.ModelType}s
 * are configured for persistence (using JPA, cache, or both).
 * </p>
 */
public enum VaultiqPersistenceMode {
    /**
     * Indicates that the data model should be persisted using only JPA (database).
     * Cache is not expected to be the primary storage for this mode.
     */
    JPA_ONLY,

    /**
     * Indicates that the data model should be persisted using only a cache.
     * JPA (database) is not expected to be the primary storage for this mode.
     */
    CACHE_ONLY,

    /**
     * Indicates that the data model should be persisted using both JPA (database) and a cache.
     * Both storage mechanisms are expected to be active for this mode.
     */
    JPA_AND_CACHE;
}
