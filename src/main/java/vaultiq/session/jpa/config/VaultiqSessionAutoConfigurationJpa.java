package vaultiq.session.jpa.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;

@Configuration(proxyBeanMethods = false)
@ConditionalOnVaultiqModelConfig(
        method = VaultiqPersistenceMethod.USE_JPA,
        type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING}
)
@EntityScan("vaultiq.session.jpa.session.model")
@EnableJpaRepositories("vaultiq.session.jpa.session.repository")
@ComponentScan(basePackages = "vaultiq.session.jpa.session.service")
public final class VaultiqSessionAutoConfigurationJpa {
}
