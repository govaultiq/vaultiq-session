package vaultiq.session.core.contracts;

@FunctionalInterface
public interface UserIdentityAware {
    String getCurrentUserID();
}
