package vaultiq.session.jpa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;

/**
 * Spring Boot autoconfiguration for enabling JPA persistence for Vaultiq session
 * and user-session mapping models.
 * <p>
 * This configuration is active only when the Vaultiq session persistence is
 * explicitly configured to use JPA for {@link ModelType#SESSION} and
 * {@link ModelType#USER_SESSION_MAPPING} via {@link ConditionalOnVaultiqModelConfig}.
 * </p>
 * <p>
 * When active, it configures JPA entity scanning, JPA repositories, and component
 * scanning for the relevant JPA-based session services.
 * </p>
 *
 * @see ConditionalOnVaultiqModelConfig
 * @see VaultiqPersistenceMethod#USE_JPA
 * @see ModelType#SESSION
 * @see ModelType#USER_SESSION_MAPPING
 * @see EntityScan
 * @see EnableJpaRepositories
 * @see ComponentScan
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnVaultiqModelConfig(
        method = VaultiqPersistenceMethod.USE_JPA,
        type = {ModelType.SESSION, ModelType.USER_SESSION_MAPPING}
)
@EntityScan("vaultiq.session.jpa.session.model")
@EnableJpaRepositories("vaultiq.session.jpa.session.repository")
@ComponentScan(basePackages = "vaultiq.session.jpa.session")
public final class VaultiqSessionAutoConfigurationJpa {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionAutoConfigurationJpa.class);

    public VaultiqSessionAutoConfigurationJpa() {
        log.info("Initializing SessionBlocklistAutoConfigurationJpa 📦");
    }
}
