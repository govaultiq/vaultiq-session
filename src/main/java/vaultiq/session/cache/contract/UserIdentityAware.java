package vaultiq.session.cache.contract;

@FunctionalInterface
public interface UserIdentityAware {

    /**
     * Returns a stringified user identifier. Implementations should convert UUIDs or numeric IDs
     * to string using standard methods like UUID.toString() or String.valueOf().
     */
    String getCurrentUserId();

}
