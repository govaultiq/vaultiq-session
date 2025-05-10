package vaultiq.session.cache.model;

public enum ModelType {
    SESSION("session-pool"),
    USER_SESSION_MAPPING("user-session-mapping"),
    USER_ACTIVITY_LOGS("user-activity-logs"),
    BLOCKLIST("blocklist");

    private final String alias;

    ModelType(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}
