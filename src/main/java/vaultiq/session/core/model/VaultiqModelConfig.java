package vaultiq.session.core.model;

import vaultiq.session.cache.model.ModelType;

import java.time.Duration;

public record VaultiqModelConfig(
        ModelType modelType,
        String cacheName,
        boolean useJpa,
        boolean useCache,
        Duration syncInterval
) {
}
