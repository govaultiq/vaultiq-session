package vaultiq.session.jpa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "vaultiq.session.persistence.jpa", name = "enabled", havingValue = "true")
@EntityScan("vaultiq.session.jpa.model")
@EnableJpaRepositories("vaultiq.session.jpa.repository")
public final class VaultiqJpaAutoConfiguration {
}
