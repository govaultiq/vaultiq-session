package vaultiq.session.jpa.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import vaultiq.session.domain.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;

/**
 * Spring Boot autoconfiguration for enabling JPA persistence specifically for the
 * Vaultiq session revoke model.
 * <p>
 * This configuration is active only when the Vaultiq session persistence is
 * explicitly configured to use JPA for the {@link ModelType#REVOKE} via
 * the {@link ConditionalOnVaultiqModelConfig} annotation.
 * </p>
 * <p>
 * When active, it automatically configures:
 * <ul>
 * <li>JPA entity scanning for revoke-related entities.</li>
 * <li>JPA repository scanning for revoke repositories.</li>
 * <li>Component scanning for JPA-based revoke services.</li>
 * </ul>
 * </p>
 *
 * @see ConditionalOnVaultiqModelConfig
 * @see VaultiqPersistenceMethod#USE_JPA
 * @see ModelType#REVOKE
 * @see EntityScan
 * @see EnableJpaRepositories
 * @see ComponentScan
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnVaultiqModelConfig(
        method = VaultiqPersistenceMethod.USE_JPA,
        type = ModelType.REVOKE
)
@EntityScan("vaultiq.session.jpa.revoke.model")
@EnableJpaRepositories("vaultiq.session.jpa.revoke.repository")
@ComponentScan(basePackages = "vaultiq.session.jpa.revoke")
public class SessionRevocationAutoConfigurationJpa {
}
