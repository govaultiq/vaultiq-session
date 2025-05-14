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
        type = ModelType.BLOCKLIST
)
@EntityScan("vaultiq.session.jpa.blocklist.model")
@EnableJpaRepositories("vaultiq.session.jpa.blocklist.repository")
@ComponentScan(basePackages = "vaultiq.session.jpa.blocklist.service")
public class SessionBlocklistAutoConfigurationJpa {
}
