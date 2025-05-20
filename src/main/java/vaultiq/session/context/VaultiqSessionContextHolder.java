package vaultiq.session.context;

public final class VaultiqSessionContextHolder {

    private static VaultiqSessionContext context;

    private VaultiqSessionContextHolder() {
        // Avoiding External instantiation
    }

    public static VaultiqSessionContext getContext() {
        return context;
    }

    public static void setContext(VaultiqSessionContext context) {
        VaultiqSessionContextHolder.context = context;
    }

    public static void clearContext() {
        VaultiqSessionContextHolder.context = null;
    }

}
