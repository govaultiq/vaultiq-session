package vaultiq.session.core.util;

import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.model.VaultiqModelConfig;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public final class VaultiqModelConfigEnhancer {

    public static List<VaultiqModelConfig> enhance(VaultiqSessionProperties props) {
        var zenMode = props.isZenMode();
        var global = props.getPersistence().getCacheConfig();

        return props.getPersistence().getModels().stream()
                .map(model -> {
                    ModelType type = model.getType();

                    boolean useJpa = resolve(model.getUseJpa(), global != null ? global.isUseJpa() : null, zenMode);
                    boolean useCache = resolve(model.getUseCache(), global != null ? global.isUseCache() : null, zenMode);

                    Duration syncInterval = Optional.ofNullable(model.getSyncInterval())
                                    .orElseGet(() -> props.getPersistence().getCacheConfig().getSyncInterval());

                    String cacheName = model.getCacheName();
                    if (cacheName == null || cacheName.isBlank()) {
                        cacheName = type.name().toLowerCase().replace("_", "-");
                    }

                    return new VaultiqModelConfig(type, cacheName, useJpa, useCache, syncInterval);
                })
                .toList();
    }

    private static boolean resolve(Boolean specific, Boolean global, boolean zenMode) {
        if (specific != null) return specific;
        if (global != null) return global;
        return zenMode;
    }
}
