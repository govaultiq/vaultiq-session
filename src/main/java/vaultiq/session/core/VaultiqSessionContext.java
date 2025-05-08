package vaultiq.session.core;

import org.springframework.stereotype.Component;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.model.VaultiqModelConfig;
import vaultiq.session.core.util.VaultiqModelConfigEnhancer;

import java.util.List;

@Component
public class VaultiqSessionContext {

    private final String cacheManagerName;
    private final List<VaultiqModelConfig> modelConfigs;
    public VaultiqSessionContext(VaultiqSessionProperties props) {
        this.cacheManagerName = props.getPersistence().getCacheConfig().getManager();
        this.modelConfigs = VaultiqModelConfigEnhancer.enhance(props);
    }

    public String getCacheManagerName() {
        return cacheManagerName;
    }

    public List<VaultiqModelConfig> getModelConfigs() {
        return modelConfigs;
    }
}
