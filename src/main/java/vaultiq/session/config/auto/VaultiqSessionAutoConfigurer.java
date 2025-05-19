package vaultiq.session.config.auto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.model.VaultiqModelConfig;
import vaultiq.session.core.util.VaultiqSessionContext;

import java.util.Arrays;

@Configuration
@ComponentScan(basePackages = {
        "vaultiq.session.cache",
        "vaultiq.session.config",
        "vaultiq.session.core",
        "vaultiq.session.fingerprint",
})
@EnableConfigurationProperties(VaultiqSessionProperties.class)
public class VaultiqSessionAutoConfigurer {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionAutoConfigurer.class);

    @Bean
    VaultiqSessionContext vaultiqSessionContext(VaultiqSessionProperties props) {
        var context = new VaultiqSessionContext(props);

        log.info("VaultiqSessionContext initialized with - isUsingJpa: {}, isUsingCache: {}, cache-manager-name: {}",
                context.isUsingJpa(), context.isUsingCache(), context.getCacheManagerName());

        var models = Arrays.stream(ModelType.values()).map(context::getModelConfig)
                .map(VaultiqModelConfig::toString)
                .toList();
        log.info("Using zen-mode={}, Resolved Model Configs: {}", props.isZenMode(), models);
        return context;
    }
}
